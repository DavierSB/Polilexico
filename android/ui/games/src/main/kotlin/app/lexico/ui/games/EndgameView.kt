package app.lexico.ui.games

import app.lexico.model.Board
import app.lexico.ui.common.RankedMove

interface EndgameSession {
  suspend fun position(): ExploredPosition

  suspend fun play(move: String?): String?

  suspend fun back(steps: Int)

  suspend fun analyze(update: (LiveLines) -> Unit)
}

typealias EndgameSessions = suspend (Int) -> EndgameSession

data class ExploredPosition(
  val board: Board,
  val players: List<String>,
  val racks: List<List<String>>,
  val scores: List<Int>,
  val onTurn: Int,
  val over: Boolean,
  val path: List<StepView>,
  val ending: List<EndingRow>,
) {
  val me: Int get() = players.indexOf(ME).coerceAtLeast(0)
}

data class LiveLines(val lines: List<LineView>, val depth: Int, val exact: Boolean) {
  companion object {
    val NONE = LiveLines(emptyList(), 0, false)
  }
}

data class LineView(val move: RankedMove, val spread: Int, val pending: Boolean, val mover: String, val steps: List<StepView>, val ending: List<EndingRow>)

data class EndingRow(val who: String, val left: List<String>, val delta: Int)

data class StepView(val who: String, val move: RankedMove, val board: Board)

internal const val ME = "Tú"
