package app.lexico.game.modes.classic

import app.lexico.game.Outcome
import app.lexico.game.engine.plainTiles
import app.lexico.model.Board

/** Una partida clasica en este momento, tal como la cuenta el motor. */
data class ClassicState(
  val board: Board,
  /** Tus fichas: "A", "CH", "?"... */
  val rack: List<String>,
  val opponentTiles: Int,
  /** Las fichas del rival: solo se revelan al terminar. */
  val opponentRack: List<String>,
  val myScore: Int,
  val opponentScore: Int,
  val myTurn: Boolean,
  val bag: Int,
  /** Las fichas que no ves: la bolsa y el atril del rival. */
  val unseen: List<String>,
  val moves: List<PlayedMove>,
  /** null = partida sin tiempo. */
  val clocks: ClassicClocks?,
  val paused: Boolean,
  /** null mientras se juega. */
  val result: ClassicResult?,
  /** Por que fallo el ultimo turno del bot, si fallo. */
  val botError: String?,
)

enum class MoveKind { PLACEMENT, PASS, EXCHANGE, INVALID }

/** Una jugada hecha. `tiles`: la colocacion ("CA.A") o las fichas cambiadas ("" si no se ven). */
data class PlayedMove(
  val byMe: Boolean,
  val kind: MoveKind,
  val coords: String,
  val tiles: String,
  val tileCount: Int,
  val score: Int,
  val myTotal: Int,
  val opponentTotal: Int,
) {
  /** "H8 CA.A" (colocaciones) o las fichas, sin corchetes de digrafos, para mostrar. */
  val text: String get() = listOf(coords, plainTiles(tiles)).filter { it.isNotEmpty() }.joinToString(" ")
}

enum class Side { ME, OPPONENT }

/** Los relojes, en milisegundos; *Ms negativo = en el descuento, del que quedan *OvertimeMs. */
data class ClassicClocks(
  val myMs: Long,
  val myOvertimeMs: Long,
  val opponentMs: Long,
  val opponentOvertimeMs: Long,
  /** Cual corre; null = ninguno. */
  val running: Side?,
)

/** Como termino. `recordPath`: el registro de la partida para revisarla ("" si no se escribio). */
data class ClassicResult(val outcome: Outcome, val lostOnTime: Boolean, val recordPath: String)
