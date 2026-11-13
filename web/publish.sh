#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")"

SITE="${1:?uso: publish.sh RUTA_DEL_REPO_DEL_SITIO}"
BUILD=build/site
[ -f "$BUILD/engine/manifest.json" ] || { echo "falta build/site: ./build.sh" >&2; exit 1; }
[ -f "$SITE/version.json" ] && [ -d "$SITE/org" ] || { echo "$SITE no parece el repo del sitio" >&2; exit 1; }

for entry in "$BUILD"/*; do
  [ "$(basename "$entry")" = app ] || rm -rf "$SITE/$(basename "$entry")"
done
cp -r "$BUILD"/. "$SITE"/
rm -rf "$SITE/js" "$SITE/css"
ls -1t "$SITE/app" | tail -n +4 | while read -r old; do rm -rf "$SITE/app/$old"; done
touch "$SITE/.nojekyll"
git -C "$SITE" status --short | head -20
