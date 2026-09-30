package app.lexico.game.modes.classic

import app.lexico.game.Outcome
import app.lexico.model.Board

data class ClassicState(
  val board: Board,
  val rack: List<String>,
  val opponentTiles: Int,
  val opponentRack: List<String>,
  val myScore: Int,
  val opponentScore: Int,
  val myTurn: Boolean,
  val bag: Int,
  val unseen: List<String>,
  val moves: List<PlayedMove>,
  val clocks: ClassicClocks?,
  val paused: Boolean,
  val result: ClassicResult?,
  val botError: String?,
)

enum class MoveKind { PLACEMENT, PASS, EXCHANGE, INVALID }

data class PlayedMove(
  val byMe: Boolean,
  val kind: MoveKind,
  val coords: String,
  val tiles: String,
  val tileCount: Int,
  val score: Int,
  val myTotal: Int,
  val opponentTotal: Int,
  val text: String,
)

enum class Side { ME, OPPONENT }

data class ClassicClocks(
  val myMs: Long,
  val myOvertimeMs: Long,
  val opponentMs: Long,
  val opponentOvertimeMs: Long,
  val running: Side?,
)

data class ClassicResult(val outcome: Outcome, val lostOnTime: Boolean, val recordPath: String)
