package app.lexico.modes.duplicate

import app.lexico.game.modes.duplicate.DuplicatePhase
import app.lexico.game.modes.duplicate.DuplicateState
import app.lexico.game.modes.duplicate.RoundResult
import app.lexico.ui.duplicate.DuplicateView
import app.lexico.ui.duplicate.InvalidPlay
import app.lexico.ui.duplicate.Phase
import app.lexico.ui.duplicate.Round
import app.lexico.ui.duplicate.RoundPlay

fun duplicateView(state: DuplicateState, notice: String?): DuplicateView = DuplicateView(
  board = state.board, phase = phase(state.phase), rounds = shownRounds(state), bag = state.bag,
  notice = notice ?: state.lastError, paused = state.paused && state.phase !is DuplicatePhase.Finished,
  revealAtEnd = state.revealAtEnd, invalidPlays = state.rounds.filter { it.assumed.invalid }.map(::invalidPlay),
)

fun efficiencyTenths(state: DuplicateState): Int {
  val master = state.rounds.sumOf { it.masterScore }
  return if (master > 0) state.rounds.sumOf { it.myScore } * 1000 / master else 0
}

private fun shownRounds(state: DuplicateState): List<Round> =
  if (state.revealAtEnd && state.phase != DuplicatePhase.Finished) state.rounds.map(::assumedRound) else state.rounds.map(::round)

private fun phase(phase: DuplicatePhase): Phase = when (phase) {
  is DuplicatePhase.Waiting -> Phase.Waiting(phase.turnMs)
  is DuplicatePhase.InvalidRack -> Phase.InvalidRack(phase.rack)
  is DuplicatePhase.ManyInvalid -> Phase.ManyInvalid(phase.rack)
  is DuplicatePhase.Playing -> Phase.Playing(phase.rack, phase.remainingMs)
  is DuplicatePhase.Confirming -> Phase.Confirming(phase.rack, phase.proposal, phase.remainingMs, phase.cancelMs)
  DuplicatePhase.Finished -> Phase.Finished
}

private fun round(r: RoundResult): Round =
  Round(r.number, master = masterPlay(r), mine = RoundPlay(r.myText, r.myScore, r.myBingo), hit = r.hit)

private fun assumedRound(r: RoundResult): Round =
  Round(r.number, master = masterPlay(r), mine = RoundPlay(r.assumed.text, r.assumed.score, r.assumed.bingo), hit = r.assumed.hit)

private fun masterPlay(r: RoundResult): RoundPlay = RoundPlay(r.masterText, r.masterScore, r.masterBingo)

private fun invalidPlay(r: RoundResult): InvalidPlay = InvalidPlay(r.number, RoundPlay(r.assumed.text, r.assumed.score))
