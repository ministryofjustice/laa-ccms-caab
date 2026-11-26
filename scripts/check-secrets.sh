#!/usr/bin/env bash

set -u

repo_root=$(git rev-parse --show-toplevel 2>/dev/null) || exit 0
cd "$repo_root" || exit 1

tmp_dir=$(mktemp -d "${TMPDIR:-/tmp}/pre-commit-secrets.XXXXXX") || {
  echo "pre-commit: unable to create a temporary directory for secret checks."
  exit 1
}
trap 'rm -rf "$tmp_dir"' EXIT HUP INT TERM

blocked=0

filename_is_secret() {
  local file=$1

  case "$file" in
    .env.example|*/.env.example|.env.sample|*/.env.sample|.env.template|*/.env.template)
      return 1
      ;;
  esac

  case "$file" in
    .env|*/.env|.env.*|*/.env.*|\
    *application-secret*.yml|*application-secret*.yaml|\
    *secret*.yml|*secret*.yaml|*secret*.json|*secret*.properties|*secret*.toml|*secret*.ini|\
    *credential*.yml|*credential*.yaml|*credential*.json|*credential*.properties|*credential*.toml|*credential*.ini|\
    *private*key*.pem|*private*key*.key|id_rsa|*/id_rsa|id_ed25519|*/id_ed25519|\
    *.p12|*.pfx|*.jks|*.keystore|\
    secrets.gradle|*/secrets.gradle|secrets*.gradle|*/secrets*.gradle)
      return 0
      ;;
  esac

  return 1
}

generic_assignment_re='(password|passwd|pwd|secret|client[_-]?secret|api[_-]?key|apikey|access[_-]?token|auth[_-]?token|private[_-]?key)[[:space:]]*[:=][[:space:]"]*[^[:space:],;#]{12,}'
placeholder_re='(example|dummy|fake|test|changeme|change[-_ ]me|placeholder|replace[-_ ]me|not[-_ ]a[-_ ]secret|notasecret|xxxx|x{4,}|<[^>]+>|\$\{[^}]+\}|env[_-]?var|environment|getenv|system\.getenv|process\.env|[A-Z][A-Z0-9_]*(TOKEN|SECRET|KEY|PASSWORD)|your[-_ ]?(password|secret|token|key))'

while IFS= read -r -d '' file; do
  if filename_is_secret "$file"; then
    echo "pre-commit: blocked secret file: $file"
    blocked=1
  fi

  staged_file="$tmp_dir/staged"
  if ! git cat-file blob ":$file" > "$staged_file" 2>/dev/null; then
    continue
  fi

  if grep -aEiq -e '-----BEGIN[[:space:]]+(RSA[[:space:]]+|EC[[:space:]]+|DSA[[:space:]]+|OPENSSH[[:space:]]+|ENCRYPTED[[:space:]]+)?PRIVATE[[:space:]]+KEY-----' "$staged_file"; then
    echo "pre-commit: blocked private key content in $file"
    blocked=1
  elif grep -aEiq '(AKIA|ASIA)[0-9A-Z]{16}' "$staged_file"; then
    echo "pre-commit: blocked AWS credential content in $file"
    blocked=1
  elif grep -aEiq 'gh[pousr]_[A-Za-z0-9]{36,}' "$staged_file"; then
    echo "pre-commit: blocked GitHub token content in $file"
    blocked=1
  elif grep -aEiq 'glpat-[A-Za-z0-9_-]{20,}' "$staged_file"; then
    echo "pre-commit: blocked GitLab token content in $file"
    blocked=1
  elif grep -aEiq 'xox[baprs]-[A-Za-z0-9-]{10,}' "$staged_file"; then
    echo "pre-commit: blocked Slack token content in $file"
    blocked=1
  elif grep -aEiq '(sk|rk)_(live|test)_[A-Za-z0-9]{16,}' "$staged_file"; then
    echo "pre-commit: blocked Stripe key content in $file"
    blocked=1
  elif grep -aEiq 'AIza[0-9A-Za-z_-]{35}' "$staged_file"; then
    echo "pre-commit: blocked Google API key content in $file"
    blocked=1
  elif grep -aEiq 'npm_[A-Za-z0-9]{36,}' "$staged_file"; then
    echo "pre-commit: blocked npm token content in $file"
    blocked=1
  elif grep -aEiq 'eyJ[A-Za-z0-9_-]{10,}\.[A-Za-z0-9_-]{10,}\.[A-Za-z0-9_-]{10,}' "$staged_file"; then
    echo "pre-commit: blocked JWT content in $file"
    blocked=1
  elif grep -aEiq "$generic_assignment_re" "$staged_file"; then
    hits_file="$tmp_dir/generic-hits"
    grep -aEi "$generic_assignment_re" "$staged_file" > "$hits_file" || true
    if grep -aEiv "$placeholder_re" "$hits_file" | grep -q .; then
      echo "pre-commit: blocked credential-like content in $file"
      blocked=1
    fi
  fi
done < <(git diff --cached --name-only --diff-filter=ACMR -z)

if [ "$blocked" -ne 0 ]; then
  echo "pre-commit: remove the staged secret, then commit again."
  exit 1
fi

exit 0
