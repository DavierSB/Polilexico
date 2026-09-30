# Funcionalidades de Poliléxico

Poliléxico es Scrabble en español, sin conexión: clásica contra bots, duplicada con las reglas
oficiales, tres minijuegos y un analizador. El motor es [Macondo](https://github.com/domino14/macondo),
de Woogles.io.

<p align="center">
  <img src="screenshots/home.png" width="260" alt="Pantalla de inicio">
  <img src="screenshots/minigames.png" width="260" alt="Minijuegos">
</p>

## Clásica

Una partida contra un bot. Hay cinco rivales, de menos a más fuertes: Polimita, Sancho, Pelusa,
María M. y Gitana.

Al terminar la partida, se puede analizar jugada a jugada: el tablero de cada turno, lo que jugó
cada uno y las mejores jugadas posibles.

<p align="center">
  <img src="screenshots/classic-setup.png" width="260" alt="Elegir rival">
  <img src="screenshots/classic-placing.png" width="260" alt="Colocando una jugada">
  <img src="screenshots/classic-late.png" width="260" alt="Partida clásica avanzada">
  <img src="screenshots/classic-moves.png" width="260" alt="La planilla de movidas">
</p>

## Duplicada

Duplicada con las reglas oficiales. Al terminar la partida, también se puede analizar jugada a
jugada.

<p align="center">
  <img src="screenshots/duplicate-game.png" width="260" alt="Partida duplicada">
  <img src="screenshots/duplicate-round.png" width="260" alt="Fin de una ronda">
  <img src="screenshots/duplicate-moves.png" width="260" alt="La planilla de la duplicada">
</p>

## Minijuegos

- **Finales**: una partida ya avanzada, con pocas fichas en la bolsa, que el jugador termina contra
  Gitana. Se elige cuántas fichas quedan y la ventaja con la que se empieza, a favor del jugador o
  del rival.
- **Scrabble Sprint**: cada mano tiene un scrabble posible, que hay que encontrar antes de que se
  agote el reloj. Hay de una a cinco vidas: rendirse en una mano cuesta una y, en *single*, poner
  una palabra no válida también. Mientras se juega, se ve el récord a batir con esas opciones.
- **¿Cuántas recuerdas?** Se ve una partida durante unos pocos segundos y luego hay que armar, con
  sus letras, las palabras que más puntos hicieron. Mientras menos tiempo, más difícil recordar.
  Se juega partida tras partida hasta quedarse sin vidas: cada palabra no recordada cuesta una, y
  en *single* también poner una palabra no válida. Se guarda el récord de palabras recordadas con
  esas opciones.

<p align="center">
  <img src="screenshots/sprint-setup.png" width="260" alt="Scrabble Sprint: opciones">
  <img src="screenshots/sprint.png" width="260" alt="Scrabble Sprint">
  <img src="screenshots/sprint-reveal.png" width="260" alt="Scrabble Sprint: los scrabbles de la mano">
</p>

<p align="center">
  <img src="assets/recall.gif" width="260" alt="¿Cuántas recuerdas?: mirar la partida">
  <img src="screenshots/recall-build.png" width="260" alt="¿Cuántas recuerdas?: armar la palabra">
</p>

## Analizador

Un tablero libre: se ponen las fichas que se quiera, en el tablero o en el atril, y el motor
muestra las mejores jugadas, con sus puntos y su **equity**: el valor de la jugada considerando su
puntuación y lo que queda en la mano. Al tocar una, se dibuja en el tablero.

<p align="center">
  <img src="screenshots/analyzer.png" width="260" alt="Analizador">
</p>

## Mis partidas

- **Partidas en curso**: se guardan tras cada jugada; se pueden dejar y seguir después.
- **Mis partidas**: las terminadas, en una carpeta por modo. Cada una se puede revisar turno a turno:
  el tablero de ese momento, lo que jugó cada uno y las mejores jugadas posibles (en duplicada,
  sin equity: allí solo cuentan los puntos).

<p align="center">
  <img src="screenshots/review.png" width="260" alt="Revisión de una partida">
</p>

## Mis estadísticas

Por modo y por rival: victorias, promedio de puntos, puntuación máxima, scrabbles y la palabra más
valiosa. En duplicada, además, la efectividad y los aciertos. Un gráfico enfrenta los puntos del
jugador con los del rival en cada partida; al tocar un punto, se abre esa partida.

<p align="center">
  <img src="screenshots/stats.png" width="260" alt="Estadísticas">
</p>

## Temas

Cuatro esquemas de color, cada uno con su tablero: Polimita, Hoja, Celeste y Noche. El tema de la
app se elige en el menú lateral; en una partida contra un rival se usa el de ese rival, y también
se puede cambiar desde la partida.

<p align="center">
  <img src="screenshots/home.png" width="200" alt="Tema Polimita">
  <img src="screenshots/theme-hoja.png" width="200" alt="Tema Hoja">
  <img src="screenshots/theme-celeste.png" width="200" alt="Tema Celeste">
  <img src="screenshots/theme-noche.png" width="200" alt="Tema Noche">
</p>

## Opciones

En el menú lateral:

- **Contar los puntos al colocar**: mientras se colocan las fichas, el tablero muestra los puntos
  que valdría la jugada.
- **Mostrar las letras faltantes**: en la clásica, al tocar la bolsa se ven las fichas que el
  jugador aún no ha visto (la bolsa y el atril del rival); si se apaga, solo cuántas quedan.

<p align="center">
  <img src="screenshots/options.png" width="260" alt="Opciones">
  <img src="screenshots/classic-bag.png" width="260" alt="Las letras faltantes al tocar la bolsa">
</p>
