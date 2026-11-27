#!/usr/bin/env bash

set -euo pipefail

script_dir=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
checker="$script_dir/check-secrets.sh"
tmp_root=$(mktemp -d "${TMPDIR:-/tmp}/check-secrets-test.XXXXXX")
trap 'rm -rf "$tmp_root"' EXIT HUP INT TERM

failures=0

assert_blocked() {
  local name=$1
  local file=$2
  local content=$3
  local repo="$tmp_root/$name"

  mkdir -p "$repo"
  git -C "$repo" init -q
  mkdir -p "$(dirname "$repo/$file")"
  printf '%s' "$content" > "$repo/$file"
  git -C "$repo" add -- "$file"

  if (cd "$repo" && bash "$checker") >/dev/null 2>&1; then
    echo "FAIL: $name was allowed"
    failures=$((failures + 1))
  fi
}

assert_allowed() {
  local name=$1
  local file=$2
  local content=$3
  local repo="$tmp_root/$name"

  mkdir -p "$repo"
  git -C "$repo" init -q
  mkdir -p "$(dirname "$repo/$file")"
  printf '%s' "$content" > "$repo/$file"
  git -C "$repo" add -- "$file"

  if ! (cd "$repo" && bash "$checker") >/dev/null 2>&1; then
    echo "FAIL: $name was blocked"
    failures=$((failures + 1))
  fi
}

awsKeyFixture="AKIA""IOSFODNN7EXAMPLE"
privateKeyFixture="-----BEGIN "$'RSA '"PRIVATE KEY-----"$'\n'"test-data"$'\n'"-----END RSA PRIVATE KEY-----"
genericFixture="api""-key: ""abcdefghijklmnop1234"
jsonFixture='{"pass''word": "aB7cD9eF2gH4jK6m"}'
fieldFixture="api""_key: aB7cD9eF2gH4jK6m"
commentFixture="pass""word: aB7cD9eF2gH4jK6m # test connection"
environmentFixture="pass""word: \${OPA_SECURITY_PASSWORD}"
testFixture="pass""word: test-client-secret-1234"
singleQuotedFixture=$'\x27password\x27: \x27aB7cD9eF2gH4jK6m\x27'
literalFallbackFixture="pass""word: \$""{APP_PASSWORD:aB7cD9eF2gH4jK6m}"
lowercasePasswordFixture="pass""word: myproductionpassword"
nestedPlaceholderFixture="pass""word: \$""{OIDC_CLIENT_SECRET:\$""{AZURE_CLIENT_SECRET}}"
multiAssignmentFixture='{"pass''word": "${DB_PASSWORD}", "api''_key": "aB7cD9eF2gH4jK6m"}'
uppercaseIdentifierFixture="api""_key: AZURE_API_KEY"

assert_blocked "application-secret" "application-secret.yml" "spring: test"
assert_blocked "credential-in-other-name" "config.txt" "$awsKeyFixture"
assert_blocked "private-key" "config.txt" "$privateKeyFixture"
assert_blocked "generic-secret" "config.txt" "$genericFixture"
assert_blocked "quoted-json-secret" "config.json" "$jsonFixture"
assert_blocked "secret-field-name" "config.txt" "$fieldFixture"
assert_blocked "commented-secret" "config.txt" "$commentFixture"
assert_blocked "single-quoted-field" "config.yml" "$singleQuotedFixture"
assert_blocked "literal-placeholder-fallback" "config.yml" "$literalFallbackFixture"
assert_blocked "lowercase-password-value" "config.txt" "$lowercasePasswordFixture"
assert_blocked "multiple-assignment-line" "config.json" "$multiAssignmentFixture"
assert_allowed "normal-change" "src/main/java/Example.java" "class Example {}"
assert_allowed "placeholder-file" ".env.example" 'client-secret: ${AZURE_CLIENT_SECRET}'
assert_allowed "test-value" "src/test/resources/application.yml" "pass""word: passw0rd"
assert_allowed "environment-placeholder" "config.yml" "$environmentFixture"
assert_allowed "test-placeholder" "config.yml" "$testFixture"
assert_allowed "nested-placeholder" "config.yml" "$nestedPlaceholderFixture"
assert_allowed "uppercase-environment-identifier" "config.yml" "$uppercaseIdentifierFixture"

type_change_repo="$tmp_root/type-change"
mkdir -p "$type_change_repo"
git -C "$type_change_repo" init -q
git -C "$type_change_repo" config user.email "test@example.com"
git -C "$type_change_repo" config user.name "Secret Test"
ln -s /tmp/not-the-actual-target "$type_change_repo/config.txt"
git -C "$type_change_repo" add config.txt
git -C "$type_change_repo" commit -qm "baseline"
rm "$type_change_repo/config.txt"
printf '%s' "$privateKeyFixture" > "$type_change_repo/config.txt"
git -C "$type_change_repo" add config.txt
if (cd "$type_change_repo" && bash "$checker") >/dev/null 2>&1; then
  echo "FAIL: type-change file was allowed"
  failures=$((failures + 1))
fi

if [ "$failures" -ne 0 ]; then
  echo "$failures secret checker test(s) failed"
  exit 1
fi

echo "Secret checker tests passed"
