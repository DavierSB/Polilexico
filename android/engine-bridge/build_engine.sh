#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")"

OUT="${1:?uso: build_engine.sh OUT_DIR}"
ENGINE=../../engine
MACONDO=../../third_party/macondo
LEXICA=../../third_party/woogles-lexica
: "${ANDROID_NDK_HOME:?falta source env.sh}"
command -v gomobile >/dev/null || { echo "falta gomobile en el PATH (source env.sh)" >&2; exit 1; }
[ -f "$MACONDO/go.mod" ] || { echo "falta el submodulo: git submodule update --init" >&2; exit 1; }
[ -f "$ENGINE/go.mod" ] || { echo "falta engine/go.mod" >&2; exit 1; }

"$LEXICA"/download.sh

mkdir -p "$OUT"
OUT="$(cd "$OUT" && pwd)"
rm -rf "$OUT"/{engine.aar,classes.jar,jni,assets}

DATA="$OUT/assets/data"
mkdir -p "$DATA/lexica/gaddag" "$DATA/letterdistributions" "$DATA/strategy/default"
cp "$LEXICA"/FILE2017.{kwg,klv2} "$DATA/lexica/gaddag/"
cp "$MACONDO"/data/letterdistributions/spanish "$DATA/letterdistributions/"
cp "$MACONDO"/data/strategy/default/{preendgame.json,quackle_preendgame.json,winpct.csv} "$DATA/strategy/default/"

(
  cd "$ENGINE"
  export GOTOOLCHAIN=go1.26.1
  gomobile bind -target=android/arm64,android/arm -androidapi 24 -javapkg app.lexico.go \
      -ldflags="-s -w" -o "$OUT/engine.aar" \
      . ./events ./modes/classic ./modes/duplicate ./modes/demo ./modes/sprint ./review
)

(cd "$OUT" && unzip -q -o engine.aar classes.jar 'jni/*')
ls -la "$OUT" "$OUT"/jni/*
