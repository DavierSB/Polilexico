package app.lexico.ui.sprint

import app.lexico.model.Board
import app.lexico.model.Placement

/*
 * Lo que la pantalla de Scrabble Sprint necesita para dibujarse (SprintView) y lo que puede
 * pedir (SprintActions). La busqueda de manos, el reloj y las vidas los lleva quien arma la
 * vista; la pantalla solo los muestra.
 */

/** En que punto de la serie esta. */
sealed interface Phase {
  /** Buscando la mano siguiente. */
  data object Searching : Phase

  /** Pensando la mano; `remainingMs`, lo que queda en el reloj de la serie. */
  data class Solving(val hand: Hand, val remainingMs: Long) : Phase

  /** Mano cerrada: como fue y sus scrabbles, hasta pedir la siguiente. */
  data class Revealed(val hand: Hand, val result: HandResult) : Phase

  /** Sin tiempo o sin vidas: la ultima mano, si la hubo. */
  data class Finished(val hand: Hand?, val result: HandResult?) : Phase
}

/** Una mano: el tablero y el atril del jugador en turno. */
data class Hand(val board: Board, val rack: List<String>)

/** Como se cerro una mano: tu scrabble, si lo encontraste, y todos, de mas a menos puntos. */
data class HandResult(val outcome: Outcome, val answer: Bingo?, val bingos: List<Bingo>)

/** INVALID: en single, pusiste palabras no validas. */
enum class Outcome { SOLVED, TIMEOUT, GAVE_UP, INVALID }

/** Un scrabble en notacion FISE ("H8 CA.ADOS") con sus puntos. */
data class Bingo(val placement: String, val score: Int)

data class SprintView(
  val phase: Phase,
  val lives: Int,
  val maxLives: Int,
  /** Las manos resueltas. */
  val solved: Int,
  /** Mensaje para el jugador (una respuesta rechazada...); null = ninguno. */
  val notice: String? = null,
  /** En pausa: la mano se tapa hasta que el jugador continua. */
  val paused: Boolean = false,
  /** El record con estas opciones al empezar la serie (0 = aun no hay): el que hay que batir. */
  val best: Int = 0,
  /** Al terminar la serie, el record con sus opciones; null mientras sigue. */
  val record: Record? = null,
)

/** El record (manos resueltas) con las opciones de la serie, y si esta lo acaba de batir. */
data class Record(val best: Int, val isNew: Boolean)

/** Lo que el jugador puede pedir desde la pantalla. */
interface SprintActions {
  fun propose(placement: Placement)
  /** Rendirse en la mano: cuesta una vida. */
  fun giveUp()
  /** De la mano cerrada a la siguiente. */
  fun next()
  /** Otra serie con las mismas opciones. */
  fun restart()
  fun exit()
  fun pause()
  /** Seguir tras la pausa. */
  fun resume()
}
