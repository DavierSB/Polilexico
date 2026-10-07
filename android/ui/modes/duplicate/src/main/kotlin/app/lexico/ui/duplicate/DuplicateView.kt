package app.lexico.ui.duplicate

import app.lexico.model.Placement
import app.lexico.model.Board

sealed interface Phase {
  data class Waiting(val turnMs: Long) : Phase

  data class InvalidRack(val rack: List<String>) : Phase

  data class ManyInvalid(val rack: List<String>) : Phase

  data class Playing(val rack: List<String>, val remainingMs: Long?) : Phase

  data class Confirming(val rack: List<String>, val placement: String, val remainingMs: Long?, val cancelMs: Long) : Phase

  data object Finished : Phase
}

data class RoundPlay(val text: String, val points: Int, val bingo: Boolean = false)

data class Round(val number: Int, val master: RoundPlay, val mine: RoundPlay, val hit: Boolean)

data class DuplicateView(
  val board: Board,
  val phase: Phase,
  val rounds: List<Round>,
  val bag: List<String>,
  val notice: String? = null,
  val paused: Boolean = false,
) {
  val myScore: Int get() = rounds.sumOf { it.mine.points }
  val masterScore: Int get() = rounds.sumOf { it.master.points }
  val hits: Int get() = rounds.count { it.hit }

  val efficiency: Double get() = if (masterScore > 0) myScore * 100.0 / masterScore else 0.0

  val round: Int get() = if (phase == Phase.Finished) rounds.size else rounds.size + 1
}

interface DuplicateActions {
  fun showRack()
  fun propose(placement: Placement)
  fun pass()
  fun cancel()
  fun resign()
  fun exit()
  fun analyze()
  fun pause()
  fun resume()
}
