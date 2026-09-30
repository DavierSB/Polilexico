#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")"

grep -v '^\s*\(#\|$\)' sources.txt | while read -r name sha url; do
  if [ -f "$name" ] && echo "$sha  $name" | sha256sum -c --status; then
    continue
  fi
  echo "descargando $name"
  curl -fsSL --retry 3 -o "$name.part" "$url"
  echo "$sha  $name.part" | sha256sum -c --status \
    || { rm -f "$name.part"; echo "SHA-256 distinto en $name ($url)" >&2; exit 1; }
  mv "$name.part" "$name"
done
