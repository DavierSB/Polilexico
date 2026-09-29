# android

La app Android de Poliléxico: un proyecto Gradle con la interfaz (Compose), el juego y el puente
con el motor de [`../engine`](../engine/README.md).

## Módulos

```
engine-bridge/       :engine-bridge, compila ../engine con gomobile + fachada Kotlin (ver su README)
model/               :model, fichas, tablero y notación FISE; sin interfaz ni motor
game/                :game, las partidas en marcha: llama al motor, escucha sus avisos, guarda y traduce
                     (modes/classic, modes/duplicate, modes/recall, modes/sprint, analysis, storage, engine)
ui/common/           :ui:common, tema y piezas que comparten las partidas (barra, bolsa, diálogos)
ui/board/            :ui:board, dibujo del tablero y del atril, y colocar fichas a mano
ui/modes/classic/    :ui:modes:classic, la partida contra un bot y sus componentes; también las opciones de Finales
ui/modes/duplicate/  :ui:modes:duplicate, la duplicada contra el máster y sus componentes
ui/modes/analysis/   :ui:modes:analysis, el analizador con su paleta de letras
ui/modes/recall/     :ui:modes:recall, "¿Cuántas recuerdas?": ver una partida y armar sus palabras
ui/modes/sprint/     :ui:modes:sprint, Scrabble Sprint: el scrabble de cada mano a contrarreloj, con 3 vidas
ui/games/            :ui:games, partidas en curso, mis partidas (terminadas) y su revisión turno a turno
app/                 :app, arranque, navegación, menú lateral e inicio; ensambla todo
```

Dependencias (las hace cumplir Gradle):

```
:app ──> :ui:modes:*, :ui:games ──> :ui:board, :ui:common ──> :model
  └────> :game ──> :engine-bridge ──> ../engine (Go)
            └────> :model
```

- Nada de `ui/` depende de `:engine-bridge`. Cada modo recibe una *vista* (lo que hay que
  dibujar) y devuelve *acciones*; quien la arma no le importa.
- Los modos no se ven entre sí: lo que comparten va en `:ui:common` o `:ui:board`.
- `:game` no tiene reglas (son del motor) ni nada visual: publica el estado de cada partida,
  que vuelve a leer con cada aviso del motor, la guarda tras cada cambio en `files/in_progress`
  y la pausa cuando deja de verse. Es lo único que ve las clases de gomobile.
- `:app` une las dos partes: en `app.lexico.modes` traduce el estado de `:game` a la vista de
  cada pantalla, y las acciones de la pantalla a llamadas a `:game`.
- Solo ARM: el motor nativo no existe para x86, así que la app no corre en un emulador x86.
- Los rivales (`ui/modes/classic/src/main/res`) son personajes propios: la polimita es una foto
  CC0 de Wikimedia Commons; la Gitana, la *Gitana tropical* (1929) de Víctor Manuel; Sancho,
  Pelusa y María M. son ilustraciones generadas con IA para el proyecto.
  La fuente del logo es Mulish (SIL OFL).

## Compilar

Desde esta carpeta:

```bash
source ../env.sh
./gradlew :engine-bridge:assembleDebug        # el motor (~3 min la primera vez)
./gradlew --offline :app:assembleDebug        # la app → app/build/outputs/apk/debug/app-debug.apk
./gradlew --offline testDebugUnitTest         # pruebas de todos los módulos
```

`build.gradle.kts` sube algunas dependencias transitivas de Compose a las versiones que ya están
en el `.gradle-home` compartido, para poder compilar con `--offline`.

## Release

La firma sale de `keystore.properties` (en esta carpeta, fuera de git):

```properties
storeFile=/ruta/a/polilexico-release.jks
storePassword=...
keyAlias=polilexico
keyPassword=...
```

```bash
./gradlew --offline :app:assembleRelease      # → app/build/outputs/apk/release/
```

Salen tres APK: uno por arquitectura (`arm64-v8a`, `armeabi-v7a`) y uno universal. Su
`versionCode` es `appVersionCode * 10` más 2, 1 o 0; `appVersionCode` y `appVersionName` están al
principio de `app/build.gradle.kts` y suben en cada versión publicada. Sin `keystore.properties`
los APK salen sin firmar.
