#!/usr/bin/env bash
# Fails if network endpoints are hardcoded anywhere but :core:config — the ONLY place env-specific
# hosts/URLs are allowed (they flow in through BuildKonfig per -Penv). Keeps mainnet endpoints out
# of the general codebase. Run from CI and locally.
set -euo pipefail

root="$(cd "$(dirname "$0")/.." && pwd)"

# Source roots to scan (skip build output, the config module, and generated code).
roots=(core feature messaging lightning shared androidApp iosApp)

# Suspicious endpoint signals: URL schemes, .onion hosts, or a domain label immediately followed by
# a token boundary (so "androidx.compose" / ".compileSdk" do NOT match, but "foo.com:50002" does).
pattern='(https?|wss?|ssl|tcp)://|[a-z0-9-]+\.onion([/:"'"'"' )]|$)|[a-z0-9-]+\.(com|net|org|io|invalid)([/:"'"'"' )]|$)'

hits=""
for r in "${roots[@]}"; do
  dir="$root/$r"
  [ -d "$dir" ] || continue
  found="$(grep -RInE "$pattern" \
      --include='*.kt' --include='*.kts' --include='*.swift' \
      --exclude-dir=build --exclude-dir='.gradle' --exclude-dir='generated' \
      "$dir" 2>/dev/null \
    | grep -vE '/core/config/' \
    || true)"
  [ -n "$found" ] && hits="$hits$found"$'\n'
done

if [ -n "${hits// /}" ] && [ -n "$(printf '%s' "$hits" | tr -d '[:space:]')" ]; then
  echo "ERROR: hardcoded endpoint(s) found outside :core:config. Move them into BuildKonfig:" >&2
  printf '%s\n' "$hits" >&2
  exit 1
fi

echo "no-hardcoded-endpoints: OK (no endpoints outside :core:config)"
