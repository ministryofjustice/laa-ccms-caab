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

aws_key="AKIA""IOSFODNN7EXAMPLE"
private_key="-----BEGIN "$'RSA '"PRIVATE KEY-----"$'\n'"test-data"$'\n'"-----END RSA PRIVATE KEY-----"
generic_secret="api""-key: ""abcdefghijklmnop1234"

assert_blocked "application-secret" "application-secret.yml" "spring: test"
assert_blocked "credential-in-other-name" "config.txt" "$aws_key"
assert_blocked "private-key" "config.txt" "$private_key"
assert_blocked "generic-secret" "config.txt" "$generic_secret"
assert_allowed "normal-change" "src/main/java/Example.java" "class Example {}"
assert_allowed "placeholder-file" ".env.example" 'client-secret: ${AZURE_CLIENT_SECRET}'
assert_allowed "test-value" "src/test/resources/application.yml" "password: passw0rd"

if [ "$failures" -ne 0 ]; then
  echo "$failures secret checker test(s) failed"
  exit 1
fi

echo "Secret checker tests passed"
