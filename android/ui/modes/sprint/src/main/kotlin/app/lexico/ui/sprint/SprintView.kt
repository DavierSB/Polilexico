package app.lexico.ui.sprint

import app.lexico.model.Board
import app.lexico.model.Placement

sealed interface Phase {
  data object Searching : Phase

  data class Solving(val hand: Hand, val remainingMs: Long) : Phase

  data class Revealed(val hand: Hand, val result: HandResult) : Phase

  data class Finished(val hand: Hand?, val result: HandResult?) : Phase
}

data class Hand(val board: Board, val rack: List<String>)

data class HandResult(val outcome: Outcome, val answer: Bingo?, val bingos: List<Bingo>)

enum class Outcome { SOLVED, TIMEOUT, GAVE_UP, INVALID }

data class Bingo(val placement: String, val score: Int, val text: String)

data class SprintView(
  val phase: Phase,
  val lives: Int,
  val maxLives: Int,
  val solved: Int,
  val notice: String? = null,
  val paused: Boolean = false,
  val best: Int = 0,
  val record: Record? = null,
)

data class Record(val best: Int, val isNew: Boolean)

interface SprintActions {
  fun propose(placement: Placement)
  fun giveUp()
  fun next()
  fun restart()
  fun exit()
  fun pause()
  fun resume()
}
