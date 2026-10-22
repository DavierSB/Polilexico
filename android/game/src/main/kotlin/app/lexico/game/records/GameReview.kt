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
  val humanStarts: Boolean = true,
) {
  val efficiency: Double get() = if (opponentScore > 0) myScore * 100.0 / opponentScore else 0.0
}

data class GameReview(val game: FinishedGame, val turns: List<ReviewTurn>, val startTurn: Int = 0) {
  val humanStarts: Boolean get() = game.humanStarts
}

data class ReviewTurn(
  val number: Int,
  val player: String,
  val rack: List<String>,
  val board: Board,
  val candidates: List<ReviewMove>,
  val marks: List<ReviewMark>,
  val myScore: Int = 0,
  val opponentScore: Int = 0,
  val endgame: Boolean = false,
)

data class ReviewMove(val text: String, val score: Int, val equity: Double?, val placement: Placement?)

data class ReviewMark(val who: String, val move: ReviewMove, val rank: Int?)

data class EndgameReview(val lines: List<EndgameLine>, val depth: Int = 0, val exact: Boolean = false)

data class EndgameLine(val spread: Int, val steps: List<LineStep>, val ending: LineEnding, val pending: Boolean = false) {
  val move: ReviewMove get() = steps.first().move
}

data class LineStep(val move: ReviewMove, val board: Board)

data class LineEnding(val moverLeft: List<String>, val otherLeft: List<String>, val moverDelta: Int, val otherDelta: Int)

data class ExplorerPosition(
  val board: Board,
  val racks: List<List<String>>,
  val scores: List<Int>,
  val onTurn: Int,
  val over: Boolean,
  val path: List<LineStep>,
  val ending: LineEnding,
)
