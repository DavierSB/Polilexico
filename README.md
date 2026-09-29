# Poliléxico

Poliléxico is the first Spanish-language offline Scrabble app to bring together both game modes, classic and
duplicate, with all their rules, plus a few mini-games. Its game engine is
[Macondo](https://github.com/domino14/macondo), by Woogles.io.

<p align="center">
  <img src="docs/screenshots/home.png" width="200" alt="Home">
  <img src="docs/screenshots/classic-game.png" width="200" alt="Classic game">
  <img src="docs/screenshots/duplicate-game.png" width="200" alt="Duplicate game">
  <img src="docs/screenshots/analyzer.png" width="200" alt="Analyzer">
</p>

A tour of every mode, with more screenshots (in Spanish, like the app): [docs/FEATURES.md](docs/FEATURES.md).

This repository contains the app's source code.

## Building

```bash
git clone --recurse-submodules <url>
cd android
source ../env.sh
./gradlew :engine-bridge:assembleDebug     # the engine
./gradlew :app:assembleDebug               # → app/build/outputs/apk/debug/
./gradlew :app:assembleRelease             # → app/build/outputs/apk/release/
```

`env.sh` expects the Android SDK and NDK, Go and gomobile in `../Android`; to use another path,
`export LEXICO_ENV=/path` before the `source`. The release APK is signed with
`android/keystore.properties`; without it, the APK is unsigned.

## Release

Published APKs are available under [Releases](../../releases). They are signed with this
certificate (SHA-256), which you can check with `apksigner verify --print-certs` or AppVerifier:

```
37:84:CA:E1:68:0B:BA:D4:6C:F5:38:B2:B0:72:D4:5F:D5:86:19:14:61:23:21:B0:0D:33:0D:87:73:B0:C9:F6
```

## License

© 2026 Davier Bello. GPL-3.0-or-later, with the [additional terms](ADDITIONAL_TERMS.md) allowed by
its section 7.
