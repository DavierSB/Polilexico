package app.lexico.game.modes.sprint

import app.lexico.model.Board

data class SprintSetup(val totalMs: Long, val lives: Int, val invalidCostsLife: Boolean = true, val difficulty: String = "normal")

data class SprintState(
  val phase: SprintPhase,
  val lives: Int,
  val maxLives: Int,
  val solved: Int,
  val posed: Int,
  val paused: Boolean,
  val lastError: String?,
)

sealed interface SprintPhase {
  data object Searching : SprintPhase

  data class Solving(val hand: Hand, val remainingMs: Long) : SprintPhase

  data class Revealed(val hand: Hand, val result: HandResult) : SprintPhase

  data class Finished(val hand: Hand?, val result: HandResult?) : SprintPhase
}

data class Hand(val board: Board, val rack: List<String>, val bingoCount: Int)

data class HandResult(val outcome: HandOutcome, val answer: Bingo?, val bingos: List<Bingo>)

enum class HandOutcome { SOLVED, TIMEOUT, GAVE_UP, INVALID }

data class Bingo(val placement: String, val score: Int, val text: String)
