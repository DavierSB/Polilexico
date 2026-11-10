# web

La versión web de Poliléxico: HTML, CSS y JavaScript sin dependencias ni paso de compilación,
más el mismo motor ([`engine/`](../engine/README.md)) compilado a WebAssembly.

Trae la clásica contra los cinco rivales y el juego en línea (cuentas, ofertas, retos, partidas,
espectadores y chat) contra el mismo servidor que la app.

```
web/
├── build.sh            arma build/site: copia site/, compila el motor y lo parte en trozos
├── wasm/               el puente Go → JavaScript (módulo lexico/web/wasm)
│   └── slim/           sustitutos de macondo para la compilación web (go build -overlay)
├── test/               comparan la puntuación y la lectura en línea de JavaScript con el motor
└── site/               lo que se publica
    ├── index.html
    ├── css/            temas como variables CSS; el tablero escala con unidades cqw
    ├── engine/         worker.js (descarga y arranca el motor) y memfs.js
    └── js/
        ├── engine.js   habla con el Worker: call(nombre, ...args) → Promise
        ├── score.js    la puntuación de una jugada, sin motor
        ├── classic/    configuración, partida, atril, arrastre y teclado
        ├── online/     API HTTP, WebSocket, lobby, cuenta, sala y mirror.js
        └── screens/    inicio y demo
```

## Qué se carga y cuándo

- **Al abrir** (~200 KB comprimidos): la interfaz y todo lo en línea. Jugar en línea no necesita
  el motor: `online/mirror.js` lee la partida que manda el servidor (las jugadas, los atriles, los
  relojes y el descuento) y `score.js` calcula la puntuación en vivo.
- **En segundo plano**, 1,5 s después: el motor y el diccionario (~12,6 MB comprimidos), en
  trozos de 1 MB que se guardan uno a uno en la caché del navegador (Cache API). Si la descarga se
  corta, se reintenta sola; si se cierra la página, sigue desde el último trozo la próxima vez.
  Al cambiar la versión, se borran los trozos viejos.
- Mientras tanto, Clásica y «Volver a la partida» se ven bloqueados con el porcentaje.

## Cómo corre el motor

- `worker.js` junta los trozos, los descomprime (`DecompressionStream`) y arranca
  `engine.wasm` en un Web Worker, para que el bot piense sin congelar la página.
- Go lee el diccionario con `os.Open`; `memfs.js` le ofrece un sistema de archivos en memoria con
  la interfaz de Node que espera `syscall/js`.
- Cada llamada es asíncrona: el Worker pide (`lexicoCall`), Go responde en su goroutine
  (`lexicoReply`) y avisa de los cambios de una partida (`lexicoChanged`). Una llamada que
  esperara a JavaScript dentro de otra se quedaría bloqueada.
- `wasm/slim` reemplaza al compilar el cliente de Triton y la evaluación con redes neuronales de
  macondo, que solo usa el bot FAST_ML (no lo tiene Poliléxico) y solo funciona en inglés. Así el
  motor baja de 51 a 26 MB (de 9 a 5,7 MB comprimido). `third_party` no se toca.
- `wasm_exec.js` sale del mismo Go que compila el motor (1.26). Si se mezclan versiones, el motor
  no arranca.

## Compilar y probar

```bash
source ../env.sh
./build.sh                         # → build/site
python3 -m http.server 8765 --directory build/site
```

Se abre en `http://localhost:8765`. Para usar otro servidor de partidas:
`http://localhost:8765/?server=http://localhost:8080` (queda guardado en el navegador).

Las pruebas usan `build/site` (hay que compilar antes) y comparan con el motor real:

```bash
node test/score.test.mjs 5      # puntuación: las mejores jugadas del motor y colocaciones al azar
node test/mirror.test.mjs 6     # lectura en línea: cada turno, desde los dos asientos
node test/penalty.test.mjs      # descuento por tiempo en los bordes de cada minuto
```

## Publicar

`build/site` es un sitio estático: sirve en GitHub Pages o en cualquier hosting. Los trozos van ya
comprimidos (`.bin`), así que el hosting no necesita comprimir nada. El servidor de partidas acepta
peticiones de cualquier origen ([`server/cors.go`](../server/cors.go)).
