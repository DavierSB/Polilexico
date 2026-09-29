# :engine-bridge

El puente entre el motor ([`../../engine`](../../engine/README.md), en Go) y Android: lo compila
con gomobile como biblioteca nativa y ofrece una fachada Kotlin. Es la única pieza de Android
que ve las clases que genera gomobile.

```
engine-bridge/
├── build_engine.sh    gomobile bind de ../../engine → build/gomobile/{classes.jar, jni/, assets/data}
├── build.gradle.kts   biblioteca Android :engine-bridge; la tarea buildEngineGo llama al script
├── consumer-rules.pro deja intactas las clases que gomobile genera
└── src/main/kotlin/app/lexico/engine/WooglesEngine.kt   la fachada
```

Recorrido:

```
third_party/macondo ──importa── engine/*.go ──gomobile bind──> classes.jar + .so + assets/data
                                                                    │
                                             WooglesEngine.kt ──────┘  ← :game, :app…
```

- Las clases Java generadas quedan en `app.lexico.go.<paquete Go>` (`-javapkg app.lexico.go`):
  `engine.Engine`, `classic.Classic.start(bot)` → `classic.Game`, `duplicate.Duplicate.start()`
  → `duplicate.Game`, `demo.Demo.play()`, `sprint.Sprint.newMatch(handMs, listener)`.
- Solo ARM (arm64-v8a y armeabi-v7a), `minSdk` 24.
- Solo se empaquetan los datos que usan HastyBot y el máster (~9 MB). El `.wmp` de 243 MB
  (bot élite) y los modelos de red neuronal quedan fuera.
- `build_engine.sh` fija `GOTOOLCHAIN=go1.26.1`: gomobile escribe `go 1.26` en su go.mod
  temporal y Go intentaría bajar un toolchain `go1.26` que no existe.
- El diccionario (.kwg / .klv2) no se versiona: `build_engine.sh` lo descarga con
  `../../third_party/woogles-lexica/download.sh`.

## Compilar

```bash
source ../../env.sh
../gradlew :engine-bridge:assembleDebug   # corre buildEngineGo solo si cambió algo (~3 min)
```
