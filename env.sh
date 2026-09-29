#!/usr/bin/env bash
# Entorno de compilacion: SDK y NDK de Android, Go y gomobile.
# Uso:  source ./env.sh
#
# Por defecto reutiliza el entorno aislado de ../Android (sdk/, .gradle-home/, .go-home/).
# Para usar otro, exportar LEXICO_ENV=/ruta/a/esa/carpeta antes del source.
_LEXICO_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
_ENV_DIR="${LEXICO_ENV:-$(cd "$_LEXICO_DIR/../Android" && pwd)}"

export ANDROID_HOME="$_ENV_DIR/sdk"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
export ANDROID_USER_HOME="$_ENV_DIR/.android-home"
export ANDROID_NDK_HOME="$ANDROID_HOME/ndk/29.0.14206865"
export GRADLE_USER_HOME="$_ENV_DIR/.gradle-home"
export PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH"
# dl.google.com responde 404 en su endpoint IPv4 (bug de CDN detectado el 2026-09-07).
export JAVA_TOOL_OPTIONS="-Djava.net.preferIPv6Addresses=true"

# Go: cache de modulos, de compilacion y binarios (gomobile, gobind).
export GOPATH="$_ENV_DIR/.go-home"
export GOMODCACHE="$GOPATH/pkg/mod"
export GOCACHE="$GOPATH/cache"
export GOBIN="$GOPATH/bin"
export GOFLAGS="-modcacherw"
export PATH="$GOBIN:$PATH"

unset _LEXICO_DIR _ENV_DIR
