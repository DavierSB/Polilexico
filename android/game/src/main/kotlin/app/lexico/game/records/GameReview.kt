package app.lexico.game.records

import app.lexico.game.Outcome
import app.lexico.game.storage.Mode
import app.lexico.model.Board
import app.lexico.model.Placement

/**
 * Una partida terminada, en resumen: su registro (`path`), la modalidad, cuando empezo, el rival
 * (el bot o el master), el marcador, como termino y, en duplicada, los aciertos. De tus
 * colocaciones: los scrabbles, la palabra que mas puntos hizo y la mas larga ("" si ninguna).
 */
data class FinishedGame(
  val path: String,
  val mode: Mode,
  val startedAt: String,
  val opponent: String,
  val myScore: Int,
  val opponentScore: Int,
  val outcome: Outcome,
  val hits: Int = 0,
  val bingos: Int = 0,
  val bestWord: String = "",
  val bestWordScore: Int = 0,
  val longestWord: String = "",
  /** Cuantos turnos tuvo. */
  val turns: Int = 0,
  /** Cuando termino (la hora de su registro), en milisegundos. */
  val finishedAt: Long = 0,
) {
  /** Tus puntos sobre los del rival, en % (en duplicada, la eficiencia frente al master). */
  val efficiency: Double get() = if (opponentScore > 0) myScore * 100.0 / opponentScore else 0.0
}

/**
 * Una partida terminada, turno a turno, para revisarla. `startTurn`: donde empieza la revision
 * (desde 0); en Finales, el primer turno que jugaste tu.
 */
data class GameReview(val game: FinishedGame, val turns: List<ReviewTurn>, val startTurn: Int = 0)

/**
 * Un turno: de quien era ("" en duplicada), su atril, el tablero antes de jugar, las mejores
 * jugadas segun el motor y lo que se jugo de verdad.
 */
data class ReviewTurn(
  val number: Int,
  val player: String,
  val rack: List<String>,
  val board: Board,
  val candidates: List<ReviewMove>,
  val marks: List<ReviewMark>,
)

/** Una jugada: "H8 CASA" o "(Pasar)", sus puntos, su valoracion (si la hay) y la colocacion. */
data class ReviewMove(val text: String, val score: Int, val equity: Double?, val placement: Placement?)

/** Lo que jugo alguien ("Tú", el bot, "Máster") y su puesto entre las mejores (null = fuera). */
data class ReviewMark(val who: String, val move: ReviewMove, val rank: Int?)
