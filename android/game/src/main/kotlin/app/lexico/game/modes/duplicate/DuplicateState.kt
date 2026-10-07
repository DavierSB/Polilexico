package app.lexico.game.modes.duplicate

import app.lexico.model.Board

data class DuplicateState(
  val board: Board,
  val phase: DuplicatePhase,
  val rounds: List<RoundResult>,
  val bag: List<String>,
  val paused: Boolean,
  val lastError: String?,
  val recordPath: String?,
)

sealed interface DuplicatePhase {
  data class Waiting(val turnMs: Long) : DuplicatePhase
  data class InvalidRack(val rack: List<String>) : DuplicatePhase
  data class ManyInvalid(val rack: List<String>) : DuplicatePhase
  data class Playing(val rack: List<String>, val remainingMs: Long) : DuplicatePhase
  data class Confirming(val rack: List<String>, val proposal: String, val remainingMs: Long, val cancelMs: Long) : DuplicatePhase
  data object Finished : DuplicatePhase
}

data class RoundResult(
  val number: Int,
  val masterText: String,
  val masterScore: Int,
  val myText: String,
  val myScore: Int,
  val hit: Boolean,
  val masterBingo: Boolean = false,
  val myBingo: Boolean = false,
)
