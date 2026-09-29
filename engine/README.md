# engine

El motor de Lexico, en Go: el de Woogles ([macondo](https://github.com/domino14/macondo)) con una
API pensada para cualquier plataforma. No reimplementa Scrabble: importa `game`, `movegen`,
`ai/bot`, `turnplayer`… de macondo y los expone en llamadas sueltas que una interfaz puede hacer
una a una.

No sabe nada de Android ni de la web. Cada plataforma tiene su puente (en Android,
[`android/engine-bridge`](../android/engine-bridge/README.md), con gomobile).

## Estructura (módulo Go `lexico/engine`)

- `engine.go`, `position.go`: la API pura (`Init`, `IsValidWord`, `PlacementScore`, `BestMoves`).
- `modes/classic`, `modes/duplicate`, `modes/demo`, `modes/sprint`: un paquete por modalidad de
  juego. En clásica y duplicada, `Game` son las reglas de la partida y `Match` la partida en
  marcha: sus relojes y plazos, el bot que juega solo, la pausa y el guardado con el tiempo
  gastado. En Scrabble Sprint (`sprint`), `Match` es la serie: HastyBot busca manos con scrabble
  posible en su propio hilo, y la serie lleva el reloj de cada mano, la pausa y las vidas.
  Finales está en `classic` (`EndgameSearch`): HastyBot juega contra sí mismo hasta que la bolsa
  baja por primera vez a N fichas y, si el jugador en turno tiene la ventaja pedida, esa partida
  sigue como una clásica tuya contra HastyBot (su registro lleva `"mode":"endgame"`).
- `events`: `Listener`, el aviso con el que `Match` le dice a la plataforma que algo cambió.
- `review`: lee el registro de una partida terminada (el `-log.json`) turno a turno, con el
  tablero de cada momento, las mejores jugadas y lo que jugó cada uno.
- `internal/core`: lo que comparten (bots, HastyBot contra sí mismo en `SelfPlay`, lectura de jugadas, notación FISE, registros). Al
  estar en `internal/`, ningún puente lo expone.
- `internal/testenv`: arma la carpeta `data` para `go test` a partir de `../third_party/`.
- `internal/timing`: la hora y los temporizadores (reales, o falsos en las pruebas);
  `internal/notify`: entrega los avisos en orden, desde su propio hilo.
- `go.mod` apunta a `../third_party/macondo`.

## La API es el contrato

Todo lo exportado usa tipos simples (textos, números, estructuras planas), para que cualquier
puente lo pueda pasar tal cual: gomobile a Java, JSON a un servidor web, WebAssembly al
navegador. Las reglas (turnos, relojes, tablero, guardado) van aquí y no en las plataformas.

## Probar

```bash
source ../env.sh
go test ./...
```
