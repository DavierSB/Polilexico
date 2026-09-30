package app.lexico.ui.classic

import app.lexico.model.Placement
import app.lexico.model.Board

enum class Side { ME, OPPONENT }

sealed interface OpponentRack {
  data class Hidden(val tiles: Int) : OpponentRack
  data class Visible(val tiles: List<String>) : OpponentRack
}

data class Clock(val remainingMs: Long, val overtimeMs: Long) {
  val inOvertime: Boolean get() = remainingMs < 0
}

enum class MoveType { PLACEMENT, PASS, EXCHANGE, INVALID }

data class Move(
  val side: Side,
  val type: MoveType,
  val text: String = "",
  val tiles: Int = 0,
  val points: Int = 0,
  val myTotal: Int,
  val opponentTotal: Int,
)

data class GameEnd(
  val winner: Side?,
  val byTimeout: Boolean = false,
  val ending: Ending? = null,
  val myTimePenalty: Int = 0,
  val opponentTimePenalty: Int = 0,
) {
  val timePenalized: Boolean get() = myTimePenalty != 0 || opponentTimePenalty != 0
}

enum class EndingReason { WENT_OUT, PASSES, NEUTRAL_TURNS }

data class Ending(val reason: EndingReason, val myDelta: Int, val opponentDelta: Int)

internal fun signed(n: Int): String = if (n > 0) "+$n" else "$n"

data class ClassicView(
  val opponent: String,
  val board: Board,
  val rack: List<String>,
  val opponentRack: OpponentRack,
  val myScore: Int,
  val opponentScore: Int,
  val turn: Side?,
  val myClock: Clock? = null,
  val opponentClock: Clock? = null,
  val bag: Int,
  val unseen: List<String>,
  val moves: List<Move>,
  val end: GameEnd? = null,
  val notice: String? = null,
  val paused: Boolean = false,
) {
  val latestScore: Int? get() = moves.lastOrNull { it.type == MoveType.PLACEMENT }?.points
}

interface ClassicActions {
  fun play(placement: Placement)
  fun exchange(tiles: List<String>)
  fun pass()
  fun resign()
  fun exit()
  fun analyze()
  fun pause()
  fun resume()
}
