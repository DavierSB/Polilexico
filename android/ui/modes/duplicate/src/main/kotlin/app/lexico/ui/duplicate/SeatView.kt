package app.lexico.ui.duplicate

import app.lexico.model.Board
import app.lexico.model.Placement

enum class SeatPhase { PLAYING, RESULTS, FINISHED }

data class SeatEntry(val name: String, val text: String, val points: Int, val total: Int)

data class SeatRound(val number: Int, val entries: List<SeatEntry>)

data class SeatView(
  val me: String,
  val board: Board,
  val rack: List<String>,
  val phase: SeatPhase,
  val round: Int,
  val remainingMs: Long?,
  val proposal: SeatEntry?,
  val ready: Int,
  val waiting: Int,
  val iAmReady: Boolean,
  val live: Boolean,
  val players: List<String>,
  val rounds: List<SeatRound>,
  val bag: Int,
  val notice: String? = null,
  val status: String? = null,
) {
  val standings: List<SeatEntry>
    get() = rounds.lastOrNull()?.entries.orEmpty().sortedByDescending { it.total }
}

interface SeatActions {
  fun propose(placement: Placement)
  fun pass()
  fun ready()
  fun exit()
}

internal const val MASTER_NAME = "Máster"
