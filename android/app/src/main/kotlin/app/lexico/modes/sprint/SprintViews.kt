package app.lexico.modes.sprint

import app.lexico.game.modes.sprint.Bingo as GameBingo
import app.lexico.game.modes.sprint.Hand as GameHand
import app.lexico.game.modes.sprint.HandOutcome
import app.lexico.game.modes.sprint.HandResult as GameResult
import app.lexico.game.modes.sprint.SprintPhase
import app.lexico.game.modes.sprint.SprintState
import app.lexico.game.records.SprintRecord
import app.lexico.ui.sprint.Bingo
import app.lexico.ui.sprint.Hand
import app.lexico.ui.sprint.HandResult
import app.lexico.ui.sprint.Outcome
import app.lexico.ui.sprint.Phase
import app.lexico.ui.sprint.Record
import app.lexico.ui.sprint.SprintView

/**
 * Lo que dibuja la pantalla de Scrabble Sprint a partir del estado de la serie, el record que habia
 * al empezarla y, al terminar, su record.
 */
fun sprintView(state: SprintState, notice: String?, best: Int, record: SprintRecord?): SprintView = SprintView(
  phase = phase(state.phase), lives = state.lives, maxLives = state.maxLives, solved = state.solved, best = best,
  notice = notice ?: state.lastError, paused = state.paused, record = record?.let { Record(it.best, it.isNew) },
)

private fun phase(phase: SprintPhase): Phase = when (phase) {
  SprintPhase.Searching -> Phase.Searching
  is SprintPhase.Solving -> Phase.Solving(hand(phase.hand), phase.remainingMs)
  is SprintPhase.Revealed -> Phase.Revealed(hand(phase.hand), result(phase.result))
  is SprintPhase.Finished -> Phase.Finished(phase.hand?.let(::hand), phase.result?.let(::result))
}

private fun hand(h: GameHand): Hand = Hand(h.board, h.rack)

private fun result(r: GameResult): HandResult = HandResult(outcome(r.outcome), r.answer?.let(::bingo), r.bingos.map(::bingo))

private fun bingo(b: GameBingo): Bingo = Bingo(b.placement, b.score, b.text)

private fun outcome(o: HandOutcome): Outcome = when (o) {
  HandOutcome.SOLVED -> Outcome.SOLVED
  HandOutcome.TIMEOUT -> Outcome.TIMEOUT
  HandOutcome.GAVE_UP -> Outcome.GAVE_UP
  HandOutcome.INVALID -> Outcome.INVALID
}
