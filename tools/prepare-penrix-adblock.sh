#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
OUT_DIR="$ROOT_DIR/app/src/main/assets/privateSites/adblock"
FILTERS_OUT="$OUT_DIR/filters.txt"
RESOURCES_OUT="$OUT_DIR/resources.json"

mkdir -p "$OUT_DIR"
: > "$FILTERS_OUT"

# Curated lists only. These are the App's packaged blocking policy, not arbitrary user subscriptions.
# AdGuard Base already includes EasyList; Chinese is EasyList China + AdGuard additions.
FILTER_URLS=(
  "https://filters.adtidy.org/extension/ublock/filters/2_optimized.txt"
  "https://filters.adtidy.org/extension/ublock/filters/3_optimized.txt"
  "https://filters.adtidy.org/extension/ublock/filters/11_optimized.txt"
  "https://filters.adtidy.org/extension/ublock/filters/224_optimized.txt"
  "https://raw.githubusercontent.com/uBlockOrigin/uAssets/master/filters/filters.txt"
  "https://raw.githubusercontent.com/uBlockOrigin/uAssets/master/filters/filters-2026.txt"
  "https://raw.githubusercontent.com/uBlockOrigin/uAssets/master/filters/filters-general.txt"
  "https://raw.githubusercontent.com/uBlockOrigin/uAssets/master/filters/badware.txt"
  "https://raw.githubusercontent.com/uBlockOrigin/uAssets/master/filters/resource-abuse.txt"
  "https://raw.githubusercontent.com/uBlockOrigin/uAssets/master/filters/privacy.txt"
  "https://raw.githubusercontent.com/uBlockOrigin/uAssets/master/filters/unbreak.txt"
  "https://raw.githubusercontent.com/uBlockOrigin/uAssets/master/filters/quick-fixes.txt"
  "https://raw.githubusercontent.com/brave/adblock-lists/master/brave-lists/brave-specific.txt"
  "https://raw.githubusercontent.com/brave/adblock-lists/master/brave-lists/brave-unbreak.txt"
  "https://raw.githubusercontent.com/brave/adblock-lists/master/brave-lists/brave-android-specific.txt"
)

fetch() {
  local url="$1"
  curl --fail --location --silent --show-error \
    --retry 4 --retry-all-errors --connect-timeout 20 --max-time 120 \
    "$url"
}

for url in "${FILTER_URLS[@]}"; do
  printf '\n! ===== Penrix bundled source: %s =====\n' "$url" >> "$FILTERS_OUT"
  fetch "$url" >> "$FILTERS_OUT"
  printf '\n' >> "$FILTERS_OUT"
done

fetch "https://raw.githubusercontent.com/brave/adblock-resources/master/dist/resources.json" \
  > "$RESOURCES_OUT"

python3 - "$FILTERS_OUT" "$RESOURCES_OUT" <<'PY'
import json
import pathlib
import sys

filters = pathlib.Path(sys.argv[1])
resources = pathlib.Path(sys.argv[2])
if filters.stat().st_size < 100_000:
    raise SystemExit(f"adblock filters unexpectedly small: {filters.stat().st_size}")
with resources.open("r", encoding="utf-8") as fh:
    parsed = json.load(fh)
if not isinstance(parsed, list) or len(parsed) < 10:
    raise SystemExit("adblock resources payload is not a plausible Resource list")
print(f"Prepared {filters.stat().st_size} bytes of filters and {len(parsed)} resources")
PY
