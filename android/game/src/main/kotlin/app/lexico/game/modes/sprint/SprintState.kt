package app.lexico.game.modes.sprint

import app.lexico.model.Board

/**
 * Como sera una serie de Scrabble Sprint: el tiempo de toda la serie, las vidas al empezar y si
 * poner palabras no validas cierra la mano y cuesta una vida (single) o solo se rechaza (void).
 */
data class SprintSetup(val totalMs: Long, val lives: Int, val invalidCostsLife: Boolean = true)

/** Una serie de Scrabble Sprint en este momento, tal como la cuenta el motor. */
data class SprintState(
  val phase: SprintPhase,
  /** Las vidas que quedan, de las `maxLives` con que se empieza. */
  val lives: Int,
  val maxLives: Int,
  /** Las manos resueltas. */
  val solved: Int,
  /** Las manos propuestas (la que esta en juego incluida). */
  val posed: Int,
  val paused: Boolean,
  /** Por que fallo la busqueda de manos, si fallo. */
  val lastError: String?,
)

/** En que punto de la serie esta. Los tiempos, en milisegundos. */
sealed interface SprintPhase {
  /** HastyBot busca la mano siguiente. */
  data object Searching : SprintPhase

  /** Pensando la mano; `remainingMs`, lo que queda en el reloj de la serie. */
  data class Solving(val hand: Hand, val remainingMs: Long) : SprintPhase

  /** Mano cerrada: como fue y todos sus scrabbles, hasta pedir la siguiente. */
  data class Revealed(val hand: Hand, val result: HandResult) : SprintPhase

  /** Sin tiempo o sin vidas: la ultima mano, si la hubo. */
  data class Finished(val hand: Hand?, val result: HandResult?) : SprintPhase
}

/** Una mano: el tablero y el atril del jugador en turno, y cuantos scrabbles admiten. */
data class Hand(val board: Board, val rack: List<String>, val bingoCount: Int)

/** Como se cerro una mano: tu scrabble (si lo encontraste) y todos los posibles, de mas a menos puntos. */
data class HandResult(val outcome: HandOutcome, val answer: Bingo?, val bingos: List<Bingo>)

/** INVALID: en single, pusiste palabras no validas. */
enum class HandOutcome { SOLVED, TIMEOUT, GAVE_UP, INVALID }

/** Un scrabble: `placement` en notacion FISE ("H8 CA.ADOS"), para ponerlo en el tablero, y sus puntos. */
data class Bingo(val placement: String, val score: Int)
