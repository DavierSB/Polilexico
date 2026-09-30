package app.lexico.ui.games

import app.lexico.model.Board
import app.lexico.ui.common.RankedMove

data class ReviewView(val title: String, val turns: List<ReviewTurnView>, val startTurn: Int = 0)

data class ReviewTurnView(
  val number: Int,
  val player: String,
  val rack: List<String>,
  val board: Board,
  val candidates: List<RankedMove>,
  val marks: List<MarkView>,
)

data class MarkView(val who: String, val move: RankedMove, val rank: Int?) {
  val text: String get() = "$who: ${move.text} (${move.points})" + rankText()

  private fun rankText(): String = when (rank) {
    null -> "  · fuera de las mejores"
    0 -> "  · ¡la mejor!"
    else -> "  · puesto ${rank + 1}"
  }
}
