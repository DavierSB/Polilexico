package app.lexico.game.modes.sprint

import app.lexico.game.engine.parseBoard
import app.lexico.game.engine.rackTiles
import app.lexico.go.sprint.Match
import app.lexico.go.sprint.Solution
import app.lexico.go.sprint.Sprint

/** Lee del motor el estado de una serie de Scrabble Sprint y lo traduce a [SprintState]. */
internal class SprintReader(private val match: Match) {
  fun read(): SprintState = SprintState(
    phase = phase(), lives = match.lives().toInt(), maxLives = match.maxLives().toInt(), solved = match.solved().toInt(),
    posed = match.posed().toInt(),
    paused = match.paused(), lastError = match.lastError().ifEmpty { null },
  )

  /**
   * La fase, con el tiempo al dia. Se lee en varias llamadas y la serie puede cambiar entre
   * ellas: si falta algo, se da por buscando (el aviso del cambio traera la lectura buena).
   */
  fun phase(): SprintPhase {
    val phase = match.phase()
    val hand = hand()
    return when {
      phase == Sprint.PhaseFinished -> SprintPhase.Finished(hand, result())
      hand == null -> SprintPhase.Searching
      phase == Sprint.PhaseSolving -> SprintPhase.Solving(hand, match.remainingMs().coerceAtLeast(0))
      phase == Sprint.PhaseRevealed -> result()?.let { SprintPhase.Revealed(hand, it) } ?: SprintPhase.Searching
      else -> SprintPhase.Searching
    }
  }

  /** La mano en juego o recien cerrada; null si no hay. */
  private fun hand(): Hand? {
    val board = match.board().ifEmpty { return null }
    return Hand(parseBoard(board), rackTiles(match.rack()), match.solutionCount().toInt())
  }

  /** Como se cerro la ultima mano; null si no hay mano cerrada. */
  private fun result(): HandResult? {
    val outcome = outcomeOf(match.outcome()) ?: return null
    return HandResult(outcome, match.answer()?.let(::bingo), bingos())
  }

  private fun bingos(): List<Bingo> = (0 until match.solutionCount()).mapNotNull { match.solutionAt(it)?.let(::bingo) }

  private fun bingo(s: Solution): Bingo = Bingo("${s.coords} ${s.tiles}", s.score.toInt())

  private fun outcomeOf(text: String): HandOutcome? = when (text) {
    Sprint.OutcomeSolved -> HandOutcome.SOLVED
    Sprint.OutcomeTimeout -> HandOutcome.TIMEOUT
    Sprint.OutcomeGaveUp -> HandOutcome.GAVE_UP
    else -> null
  }
}
