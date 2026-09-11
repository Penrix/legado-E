#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
CRATE_DIR="$ROOT_DIR/native/adblock"
OUT_DIR="$ROOT_DIR/app/src/main/jniLibs"

: "${ANDROID_NDK_HOME:=${ANDROID_NDK_ROOT:-}}"
if [[ -z "${ANDROID_NDK_HOME}" ]]; then
  echo "ANDROID_NDK_HOME (or ANDROID_NDK_ROOT) is required" >&2
  exit 1
fi

cd "$CRATE_DIR"
cargo ndk \
  --platform 21 \
  -t arm64-v8a \
  -t armeabi-v7a \
  -t x86_64 \
  -o "$OUT_DIR" \
  build --release

find "$OUT_DIR" -name 'libpenrix_adblock.so' -print
