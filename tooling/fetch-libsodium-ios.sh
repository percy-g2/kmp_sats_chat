#!/usr/bin/env bash
# Vendor the prebuilt libsodium static slices used by the :core:crypto iOS cinterop. Idempotent.
# The slices are gitignored (binary), so run this once before building/testing any iOS target.
# Source: jedisct1/swift-sodium ships a committed Clibsodium.xcframework (built from libsodium's
# dist-build/apple-xcframework.sh) — no autotools / local build required.
set -euo pipefail

root="$(cd "$(dirname "$0")/.." && pwd)"
dest="$root/core/crypto/src/nativeInterop/libsodium"
device="ios-arm64_arm64e"
sim="ios-arm64_arm64e_x86_64-simulator"

if [ -f "$dest/$device/libsodium.a" ] && [ -f "$dest/$sim/libsodium.a" ]; then
  echo "libsodium iOS slices already present: $dest"
  exit 0
fi

tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT
echo "Cloning jedisct1/swift-sodium (prebuilt Clibsodium.xcframework)…"
git clone --depth 1 https://github.com/jedisct1/swift-sodium.git "$tmp/swift-sodium"

xcf="$tmp/swift-sodium/Clibsodium.xcframework"
[ -d "$xcf/$device" ] && [ -d "$xcf/$sim" ] || { echo "ERROR: expected slices not found in xcframework" >&2; exit 1; }

mkdir -p "$dest"
cp -R "$xcf/$device" "$dest/"
cp -R "$xcf/$sim" "$dest/"
echo "Vendored libsodium iOS slices into $dest"
