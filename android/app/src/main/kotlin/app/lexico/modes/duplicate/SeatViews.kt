package app.lexico.modes.duplicate

import app.lexico.game.modes.duplicate.table.TableEntry
import app.lexico.game.modes.duplicate.table.TablePhase
import app.lexico.game.modes.duplicate.table.TableState
import app.lexico.ui.duplicate.SeatEntry
import app.lexico.ui.duplicate.SeatPhase
import app.lexico.ui.duplicate.SeatRound
import app.lexico.ui.duplicate.SeatView

fun seatView(s: TableState, me: String, notice: String?, status: String? = null): SeatView = SeatView(
  me = me, board = s.board, rack = s.rack, phase = phase(s.phase), round = s.round, remainingMs = s.remainingMs,
  proposal = s.proposal?.let(::entry), ready = s.ready, waiting = s.waiting, iAmReady = s.iAmReady, live = s.live,
  players = s.players, rounds = s.rounds.map { r -> SeatRound(r.number, r.entries.map(::entry)) }, bag = s.bag, notice = notice, status = status,
)

private fun entry(e: TableEntry): SeatEntry = SeatEntry(e.name, e.text, e.points, e.total)

private fun phase(p: TablePhase): SeatPhase = when (p) {
  TablePhase.PLAYING -> SeatPhase.PLAYING
  TablePhase.RESULTS -> SeatPhase.RESULTS
  TablePhase.FINISHED -> SeatPhase.FINISHED
}
