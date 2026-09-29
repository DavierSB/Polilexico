package app.lexico.ui.games

import app.lexico.model.Board
import app.lexico.ui.common.RankedMove

/**
 * Una partida terminada para revisarla: su titulo ("Clásica · 421–485 · Ganó HastyBot"), sus
 * turnos y en cual se empieza a verla (desde 0).
 */
data class ReviewView(val title: String, val turns: List<ReviewTurnView>, val startTurn: Int = 0)

/**
 * Un turno: su numero, de quien era ("" en duplicada), el atril, el tablero antes de jugar, las
 * mejores jugadas del motor (con quien jugo cada una) y lo que se jugo de verdad.
 */
data class ReviewTurnView(
  val number: Int,
  val player: String,
  val rack: List<String>,
  val board: Board,
  val candidates: List<RankedMove>,
  val marks: List<MarkView>,
)

/** Lo que jugo alguien y en que puesto de las mejores quedo (null = fuera de la lista). */
data class MarkView(val who: String, val move: RankedMove, val rank: Int?) {
  /** "Tú: H8 CASA (12)  · ¡la mejor!" */
  val text: String get() = "$who: ${move.text} (${move.points})" + rankText()

  private fun rankText(): String = when (rank) {
    null -> "  · fuera de las mejores"
    0 -> "  · ¡la mejor!"
    else -> "  · puesto ${rank + 1}"
  }
}
