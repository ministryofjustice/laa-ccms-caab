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

assert_added_line_allowed() {
  local name=$1
  local file=$2
  local baseline=$3
  local added=$4
  local repo="$tmp_root/$name"

  mkdir -p "$repo"
  git -C "$repo" init -q
  git -C "$repo" config user.email "test@example.com"
  git -C "$repo" config user.name "Secret Test"
  mkdir -p "$(dirname "$repo/$file")"
  printf '%s\n' "$baseline" > "$repo/$file"
  git -C "$repo" add -- "$file"
  git -C "$repo" commit -qm "baseline"
  printf '%s\n%s' "$baseline" "$added" > "$repo/$file"
  git -C "$repo" add -- "$file"

  if ! (cd "$repo" && bash "$checker") >/dev/null 2>&1; then
    echo "FAIL: $name was blocked"
    failures=$((failures + 1))
  fi
}

assert_added_line_blocked() {
  local name=$1
  local file=$2
  local baseline=$3
  local added=$4
  local repo="$tmp_root/$name"

  mkdir -p "$repo"
  git -C "$repo" init -q
  git -C "$repo" config user.email "test@example.com"
  git -C "$repo" config user.name "Secret Test"
  mkdir -p "$(dirname "$repo/$file")"
  printf '%s\n' "$baseline" > "$repo/$file"
  git -C "$repo" add -- "$file"
  git -C "$repo" commit -qm "baseline"
  printf '%s\n%s' "$baseline" "$added" > "$repo/$file"
  git -C "$repo" add -- "$file"

  if (cd "$repo" && bash "$checker") >/dev/null 2>&1; then
    echo "FAIL: $name was allowed"
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
quotedHashFixture="pass""word: \"abc#DEFGHIJKLMN12\""
quotedSpaceFixture="pass""word: \"abc defghijkl mn12\""
quotedCommaFixture="pass""word: \"abc,DEFGHIJKLMN12\""
latestProductionFixture="pass""word: LatestProductionPassword!2026"
latestFallbackFixture="pass""word: \$""{LATEST_PASSWORD:aB7cD9eF2gH4jK6m}"
testPasswordFixture="pass""word = \"password\""
syntheticTokenFixture="access""-token: synthetic-invoice-token"
localSecretFixture="client""-secret: secret"
localUuidFixture="access""-token: d594f93f-e767-4b88-a9e9-2913441edfba"
gradleCodeExpressionFixture="pass""word = System.getenv(\"GITHUB_TOKEN\")?.trim()"
messageLabelFixture="contact.pass""word=pass""word"
unchangedSecretFixture="pass""word: aB7cD9eF2gH4jK6m"
unrelatedAddedLine='changed=true'
newCredentialAddedLine="pass""word: aB7cD9eF2gH4jK6m"

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
assert_blocked "quoted-hash-password" "config.yml" "$quotedHashFixture"
assert_blocked "quoted-space-password" "config.yml" "$quotedSpaceFixture"
assert_blocked "quoted-comma-password" "config.yml" "$quotedCommaFixture"
assert_blocked "latest-production-password" "config.yml" "$latestProductionFixture"
assert_blocked "latest-literal-fallback" "config.yml" "$latestFallbackFixture"
assert_allowed "normal-change" "src/main/java/Example.java" "class Example {}"
assert_allowed "placeholder-file" ".env.example" 'client-secret: ${AZURE_CLIENT_SECRET}'
assert_allowed "test-value" "src/test/resources/application.yml" "pass""word: passw0rd"
assert_allowed "environment-placeholder" "config.yml" "$environmentFixture"
assert_allowed "test-placeholder" "config.yml" "$testFixture"
assert_allowed "nested-placeholder" "config.yml" "$nestedPlaceholderFixture"
assert_allowed "uppercase-environment-identifier" "config.yml" "$uppercaseIdentifierFixture"
assert_allowed "test-password-fixture" "src/test/resources/application.yml" "$testPasswordFixture"
assert_allowed "synthetic-integration-token" "src/integrationTest/java/ExampleTest.java" "$syntheticTokenFixture"
assert_allowed "local-secret-fallback" "src/main/resources/application-local.yml" "$localSecretFixture"
assert_allowed "local-uuid-token" "src/main/resources/application-local.yml" "$localUuidFixture"
assert_allowed "gradle-code-expression" "build.gradle" "$gradleCodeExpressionFixture"
assert_allowed "message-password-label" "src/main/resources/messages.properties" "$messageLabelFixture"
assert_added_line_allowed "unchanged-secret-line" "config.txt" "$unchangedSecretFixture" "$unrelatedAddedLine"
assert_added_line_blocked "new-secret-added-line" "config.txt" "contact.email=Email" "$newCredentialAddedLine"

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
