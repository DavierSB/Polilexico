package app.lexico.modes.duplicate

import app.lexico.game.modes.duplicate.DuplicatePhase
import app.lexico.game.modes.duplicate.DuplicateState
import app.lexico.game.modes.duplicate.RoundResult
import app.lexico.ui.duplicate.DuplicateView
import app.lexico.ui.duplicate.Phase
import app.lexico.ui.duplicate.Round
import app.lexico.ui.duplicate.RoundPlay

fun duplicateView(state: DuplicateState, notice: String?): DuplicateView = DuplicateView(
  board = state.board, phase = phase(state.phase), rounds = state.rounds.map(::round), bag = state.bag,
  notice = notice ?: state.lastError, paused = state.paused,
)

private fun phase(phase: DuplicatePhase): Phase = when (phase) {
  is DuplicatePhase.Waiting -> Phase.Waiting(phase.turnMs)
  is DuplicatePhase.InvalidRack -> Phase.InvalidRack(phase.rack)
  is DuplicatePhase.Playing -> Phase.Playing(phase.rack, phase.remainingMs)
  is DuplicatePhase.Confirming -> Phase.Confirming(phase.rack, phase.proposal, phase.remainingMs, phase.cancelMs)
  DuplicatePhase.Finished -> Phase.Finished
}

private fun round(r: RoundResult): Round =
  Round(r.number, master = RoundPlay(r.masterText, r.masterScore), mine = RoundPlay(r.myText, r.myScore), hit = r.hit)
