package app.lexico.game.modes.duplicate

import app.lexico.model.Board

/** Una duplicada en este momento, tal como la cuenta el motor. */
data class DuplicateState(
  val board: Board,
  val phase: DuplicatePhase,
  /** Las rondas ya cerradas, en orden. */
  val rounds: List<RoundResult>,
  /** Las fichas que quedan en la bolsa. */
  val bag: List<String>,
  val paused: Boolean,
  /** Por que fallo la ultima ronda al anotarse, si fallo. */
  val lastError: String?,
  /** Al terminar, el registro de la partida para revisarla ("" si no se escribio); null mientras se juega. */
  val recordPath: String?,
)

/** En que punto de la ronda esta la partida. Los tiempos, en milisegundos. */
sealed interface DuplicatePhase {
  data class Waiting(val turnMs: Long) : DuplicatePhase
  data class InvalidRack(val rack: List<String>) : DuplicatePhase
  data class Playing(val rack: List<String>, val remainingMs: Long) : DuplicatePhase
  data class Confirming(val rack: List<String>, val proposal: String, val remainingMs: Long, val cancelMs: Long) : DuplicatePhase
  data object Finished : DuplicatePhase
}

/** Una ronda cerrada: la jugada del master frente a la tuya ("H8 CA.A", "pase", "tiempo agotado"...). */
data class RoundResult(
  val number: Int,
  val masterText: String,
  val masterScore: Int,
  val myText: String,
  val myScore: Int,
  val hit: Boolean,
)
