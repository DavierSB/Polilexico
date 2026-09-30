#!/usr/bin/env bash
_LEXICO_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
_ENV_DIR="${LEXICO_ENV:-$(cd "$_LEXICO_DIR/../Android" && pwd)}"

export ANDROID_HOME="$_ENV_DIR/sdk"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
export ANDROID_USER_HOME="$_ENV_DIR/.android-home"
export ANDROID_NDK_HOME="$ANDROID_HOME/ndk/29.0.14206865"
export GRADLE_USER_HOME="$_ENV_DIR/.gradle-home"
export PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH"
export JAVA_TOOL_OPTIONS="-Djava.net.preferIPv6Addresses=true"

export GOPATH="$_ENV_DIR/.go-home"
export GOMODCACHE="$GOPATH/pkg/mod"
export GOCACHE="$GOPATH/cache"
export GOBIN="$GOPATH/bin"
export GOFLAGS="-modcacherw"
export PATH="$GOBIN:$PATH"

unset _LEXICO_DIR _ENV_DIR
