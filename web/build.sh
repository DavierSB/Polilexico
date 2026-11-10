#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")"

OUT="${1:-build/site}"
MACONDO="$(cd ../third_party/macondo && pwd)"
LEXICA=../third_party/woogles-lexica
WORK=build/work
ENGINE="$OUT/engine"
PART_SIZE=1M

"$LEXICA"/download.sh

rm -rf "$OUT" "$WORK"
mkdir -p "$OUT" "$WORK/data/lexica/gaddag" "$WORK/data/letterdistributions" "$WORK/data/strategy/default"
cp -r site/. "$OUT/"

cp "$LEXICA"/FILE2017.{kwg,klv2} "$WORK/data/lexica/gaddag/"
cp "$MACONDO"/data/letterdistributions/spanish "$WORK/data/letterdistributions/"
cp "$MACONDO"/data/strategy/default/{preendgame.json,quackle_preendgame.json,winpct.csv} "$WORK/data/strategy/default/"

SLIM="$(cd wasm/slim && pwd)"
printf '{"Replace":{"%s":"%s","%s":"%s"}}\n' \
  "$MACONDO/triton/client.go" "$SLIM/triton.go.txt" \
  "$MACONDO/game/mlhelper.go" "$SLIM/mlhelper.go.txt" > "$WORK/overlay.json"
OVERLAY="$(cd "$WORK" && pwd)/overlay.json"
(cd wasm && GOWORK=off GOOS=js GOARCH=wasm go build -buildvcs=false -overlay "$OVERLAY" -trimpath -ldflags="-s -w" -o "$OLDPWD/$WORK/engine.wasm" .)
cp "$(cd wasm && GOWORK=off go env GOROOT)/lib/wasm/wasm_exec.js" "$ENGINE/"

mkdir -p "$ENGINE/parts"
FILES=$(cd "$WORK" && { echo engine.wasm; find data -type f | sort; })
VERSION=$(cd "$WORK" && cat $FILES | sha256sum | cut -c1-12)
ENTRIES=()
TOTAL=0
for f in $FILES; do
  name=$(echo "$f" | tr '/' '_')
  gzip -9 -n -c "$WORK/$f" | split -b "$PART_SIZE" -d -a 3 --additional-suffix=.bin - "$ENGINE/parts/$name."
  parts=$(ls "$ENGINE/parts/$name."*.bin | wc -l)
  bytes=$(cat "$ENGINE/parts/$name."*.bin | wc -c)
  TOTAL=$((TOTAL + bytes))
  ENTRIES+=("{\"path\":\"$f\",\"name\":\"$name\",\"parts\":$parts,\"bytes\":$bytes}")
done
printf '{"version":"%s","bytes":%s,"files":[%s]}\n' "$VERSION" "$TOTAL" "$(IFS=,; echo "${ENTRIES[*]}")" > "$ENGINE/manifest.json"

rm -rf "$WORK"
cat "$ENGINE/manifest.json"
