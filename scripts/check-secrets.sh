#!/usr/bin/env bash

set -u

repo_root=$(git rev-parse --show-toplevel 2>/dev/null) || exit 0
cd "$repo_root" || exit 1

if ! command -v python3 >/dev/null 2>&1; then
  echo "pre-commit: python3 is required to check staged secrets."
  exit 1
fi

python3 - "$repo_root" <<'PY'
import os
import re
import subprocess
import sys

repo_root = sys.argv[1]
blocked = False


def git_output(*args):
    return subprocess.run(
        ["git", "-C", repo_root, *args],
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        check=False,
    )


def filename_is_secret(path):
    normalized = path.replace("\\", "/").lower()
    name = normalized.rsplit("/", 1)[-1]

    if name in {".env.example", ".env.sample", ".env.template"}:
        return False
    if name == ".env" or name.startswith(".env."):
        return True
    secret_config_suffixes = (
        ".yml",
        ".yaml",
        ".json",
        ".properties",
        ".toml",
        ".ini",
        ".conf",
        ".config",
        ".txt",
    )
    if ("secret" in name or "credential" in name) and name.endswith(secret_config_suffixes):
        return True
    if name in {"id_rsa", "id_ed25519"}:
        return True
    if name.endswith((".p12", ".pfx", ".jks", ".keystore")):
        return True
    if name.endswith(".gradle") and name.startswith("secrets"):
        return True
    if "private" in name and "key" in name and name.endswith((".pem", ".key")):
        return True
    return False


PRIVATE_KEY_RE = re.compile(
    r"-----BEGIN\s+(?:RSA\s+|EC\s+|DSA\s+|OPENSSH\s+|ENCRYPTED\s+)?PRIVATE\s+KEY-----",
    re.IGNORECASE,
)
KNOWN_TOKEN_RES = [
    ("AWS credential", re.compile(r"(?:AKIA|ASIA)[0-9A-Z]{16}")),
    ("GitHub token", re.compile(r"gh[pousr]_[A-Za-z0-9]{36,}", re.IGNORECASE)),
    ("GitLab token", re.compile(r"glpat-[A-Za-z0-9_-]{20,}", re.IGNORECASE)),
    ("Slack token", re.compile(r"xox[baprs]-[A-Za-z0-9-]{10,}", re.IGNORECASE)),
    ("Stripe key", re.compile(r"(?:sk|rk)_(?:live|test)_[A-Za-z0-9]{16,}", re.IGNORECASE)),
    ("Google API key", re.compile(r"AIza[0-9A-Za-z_-]{35}")),
    ("npm token", re.compile(r"npm_[A-Za-z0-9]{36,}", re.IGNORECASE)),
    ("JWT", re.compile(r"eyJ[A-Za-z0-9_-]{10,}\.[A-Za-z0-9_-]{10,}\.[A-Za-z0-9_-]{10,}")),
]

CREDENTIAL_ASSIGNMENT_RE = re.compile(
    r"""
    (?<![\w-])
    ["']?
    (password|passwd|pwd|secret|client[_-]?secret|api[_-]?key|apikey|
     access[_-]?token|auth[_-]?token|private[_-]?key)
    ["']?
    \s*[:=]\s*
    (?:
        "([^"]*)" |
        '([^']*)' |
        ([^ \t\r\n,;"'#]+)
    )
    """,
    re.IGNORECASE | re.VERBOSE,
)

UPPERCASE_ENVIRONMENT_IDENTIFIER_RE = re.compile(
    r"^[A-Z][A-Z0-9_]*(?:TOKEN|SECRET|KEY|PASSWORD)$"
)

CODE_EXPRESSION_RES = [
    re.compile(r"^[A-Za-z_$][A-Za-z0-9_$.]*\s*\(", re.IGNORECASE),
    re.compile(r"^[A-Za-z_$][A-Za-z0-9_$.]*\s*\[", re.IGNORECASE),
]


def matching_outer_brace(value):
    if not value.startswith("${"):
        return -1

    depth = 0
    for index, character in enumerate(value):
        if character == "{":
            depth += 1
        elif character == "}":
            depth -= 1
            if depth == 0:
                return index
    return -1


def top_level_colon(inner):
    depth = 0
    for index, character in enumerate(inner):
        if character == "{":
            depth += 1
        elif character == "}":
            depth -= 1
        elif character == ":" and depth == 0:
            return index
    return -1


def environment_placeholder_only(value):
    value = value.strip().strip("\"'")
    if not value.startswith("${"):
        return False

    close_index = matching_outer_brace(value)
    if close_index < 0 or close_index != len(value) - 1:
        return False

    inner = value[2:close_index]
    colon_index = top_level_colon(inner)
    if colon_index < 0:
        return True

    fallback = inner[colon_index + 1:].strip()
    if not fallback:
        return True
    return environment_placeholder_only(fallback)


def is_code_expression(value):
    value = value.strip().strip("\"'")
    return any(pattern.match(value) for pattern in CODE_EXPRESSION_RES)


def is_localization_label(value, path):
    normalized = path.replace("\\", "/").lower()
    name = normalized.rsplit("/", 1)[-1]
    if not (name == "messages.properties" or (
        name.startswith("messages") and name.endswith(".properties")
    )):
        return False
    return value.strip().lower() in {"password", "password reminder"}


def is_placeholder_value(value, path):
    value = value.strip().strip("\"'")
    if environment_placeholder_only(value):
        return True
    if is_localization_label(value, path):
        return True

    lower = value.lower()
    exact_global_placeholders = {
        "example",
        "dummy",
        "fake",
        "test",
        "changeme",
        "change-me",
        "change_me",
        "placeholder",
        "replace-me",
        "replace_me",
        "not-a-secret",
        "notasecret",
        "xxxx",
    }
    if lower in exact_global_placeholders:
        return True

    delimited_fixture = re.compile(
        r"^(?:(?:test|dummy|fake|mock|sample|example|placeholder|changeme|local|synthetic)"
        r"(?:[-_.:].+)?"
        r"|.+[-_.](?:test|dummy|fake|mock|sample|example|placeholder|changeme|local|synthetic))$",
        re.IGNORECASE,
    )
    if delimited_fixture.fullmatch(value):
        return True
    if re.fullmatch(r"<[^>]+>", value):
        return True
    if UPPERCASE_ENVIRONMENT_IDENTIFIER_RE.fullmatch(value):
        return True

    lower_path = path.lower()
    test_or_local_path = (
        "/test/" in lower_path
        or "/integrationtest/" in lower_path
        or "/integration-test/" in lower_path
        or "/local/" in lower_path
        or lower_path.endswith("application-local.yml")
        or lower_path.endswith("application-local.yaml")
    )
    if test_or_local_path:
        exact_test_placeholders = {
            "password",
            "secret",
            "passw0rd",
            "bulk-claim-client",
            "synthetic-invoice-token",
            "test-access-key",
            "test-secret-key",
            "test-client-id",
            "test-client-secret",
            "local-user-details-api-token",
        }
        if lower in exact_test_placeholders:
            return True
        if re.fullmatch(
            r"[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}",
            lower,
        ):
            return True

    return False


changed_files = git_output(
    "diff",
    "--cached",
    "--name-only",
    "--diff-filter=ACMRT",
    "-z",
)
if changed_files.returncode != 0:
    print("pre-commit: unable to inspect the staged index.")
    sys.exit(1)

for raw_file in changed_files.stdout.split(b"\0"):
    if not raw_file:
        continue

    file = os.fsdecode(raw_file)
    if filename_is_secret(file):
        print(f"pre-commit: blocked secret file: {file}")
        blocked = True

    staged_diff = git_output(
        "diff",
        "--cached",
        "--unified=0",
        "--diff-filter=ACMRT",
        "--no-ext-diff",
        "--no-color",
        "--",
        file,
    )
    if staged_diff.returncode != 0:
        print("pre-commit: unable to inspect the staged diff.")
        sys.exit(1)

    text = "\n".join(
        line[1:]
        for line in staged_diff.stdout.decode("utf-8", errors="replace").splitlines()
        if line.startswith("+") and not line.startswith("+++")
    )

    if PRIVATE_KEY_RE.search(text):
        print(f"pre-commit: blocked private key content in {file}")
        blocked = True
        continue

    known_token_found = False
    for label, pattern in KNOWN_TOKEN_RES:
        if pattern.search(text):
            print(f"pre-commit: blocked {label} content in {file}")
            known_token_found = True
            blocked = True
            break
    if known_token_found:
        continue

    for match in CREDENTIAL_ASSIGNMENT_RE.finditer(text):
        unquoted_value = match.groups()[3] is not None
        value = next(
            (group for group in match.groups()[1:] if group is not None),
            "",
        ).strip()
        if len(value) < 8:
            continue
        if unquoted_value and is_code_expression(value):
            continue
        if not is_placeholder_value(value, file):
            print(f"pre-commit: blocked credential-like content in {file}")
            blocked = True
            break

if blocked:
    print("pre-commit: remove the staged secret, then commit again.")
    sys.exit(1)

sys.exit(0)
PY

exit $?
