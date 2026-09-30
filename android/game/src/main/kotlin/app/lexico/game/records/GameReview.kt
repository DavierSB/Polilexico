package app.lexico.game.records

import app.lexico.game.Outcome
import app.lexico.game.storage.Mode
import app.lexico.model.Board
import app.lexico.model.Placement

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
  val turns: Int = 0,
  val finishedAt: Long = 0,
) {
  val efficiency: Double get() = if (opponentScore > 0) myScore * 100.0 / opponentScore else 0.0
}

data class GameReview(val game: FinishedGame, val turns: List<ReviewTurn>, val startTurn: Int = 0)

data class ReviewTurn(
  val number: Int,
  val player: String,
  val rack: List<String>,
  val board: Board,
  val candidates: List<ReviewMove>,
  val marks: List<ReviewMark>,
)

data class ReviewMove(val text: String, val score: Int, val equity: Double?, val placement: Placement?)

data class ReviewMark(val who: String, val move: ReviewMove, val rank: Int?)
