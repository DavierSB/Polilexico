#!/usr/bin/env bash
# Compila el motor Go (engine/ en la raiz del repositorio, que importa macondo) con gomobile
# bind y deja en OUT lo que el modulo Android :engine-bridge necesita, ya desempaquetado del .aar:
#
#   OUT/engine.aar          el .aar tal cual lo genera gomobile (solo como referencia)
#   OUT/classes.jar         las clases Java generadas (paquete app.lexico.go.*)
#   OUT/jni/<abi>/*.so      la libreria nativa
#   OUT/assets/data/        lo que el motor lee en tiempo de ejecucion: el diccionario FILE2017
#                           (de third_party/woogles-lexica) y la distribucion y estrategia
#                           de macondo
#
# Normalmente lo llama Gradle (tarea :engine-bridge:buildEngineGo) solo si algo cambio.
# Uso a mano:  source ../../env.sh && ./build_engine.sh build/gomobile
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

# Solo lo que usan HastyBot y el master (~9 MB). El .wmp de 243 MB (bot elite) y los modelos
# de red neuronal no se incluyen.
DATA="$OUT/assets/data"
mkdir -p "$DATA/lexica/gaddag" "$DATA/letterdistributions" "$DATA/strategy/default"
cp "$LEXICA"/FILE2017.{kwg,klv2} "$DATA/lexica/gaddag/"
cp "$MACONDO"/data/letterdistributions/spanish "$DATA/letterdistributions/"
cp "$MACONDO"/data/strategy/default/{preendgame.json,quackle_preendgame.json,winpct.csv} "$DATA/strategy/default/"

(
  cd "$ENGINE"
  # gomobile escribe "go 1.26" en su go.mod temporal; sin esto Go intenta bajar un toolchain
  # "go1.26" que no existe.
  export GOTOOLCHAIN=go1.26.1
  # Cada paquete Go queda en app.lexico.go.<paquete>; internal/ no se expone.
  gomobile bind -target=android/arm64,android/arm -androidapi 24 -javapkg app.lexico.go \
      -ldflags="-s -w" -o "$OUT/engine.aar" \
      . ./events ./modes/classic ./modes/duplicate ./modes/demo ./modes/sprint ./review
)

(cd "$OUT" && unzip -q -o engine.aar classes.jar 'jni/*')
ls -la "$OUT" "$OUT"/jni/*
