package app.lexico.game.modes.sprint

import app.lexico.game.engine.moveText
import app.lexico.game.engine.parseBoard
import app.lexico.game.engine.plainTiles
import app.lexico.game.engine.rackTiles
import app.lexico.go.sprint.Match
import app.lexico.go.sprint.Solution
import app.lexico.go.sprint.Sprint
import app.lexico.model.Board

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
      phase == Sprint.PhaseFinished -> SprintPhase.Finished(hand, result(hand?.board))
      hand == null -> SprintPhase.Searching
      phase == Sprint.PhaseSolving -> SprintPhase.Solving(hand, match.remainingMs().coerceAtLeast(0))
      phase == Sprint.PhaseRevealed -> result(hand.board)?.let { SprintPhase.Revealed(hand, it) } ?: SprintPhase.Searching
      else -> SprintPhase.Searching
    }
  }

  /** La mano en juego o recien cerrada; null si no hay. */
  private fun hand(): Hand? {
    val board = match.board().ifEmpty { return null }
    return Hand(parseBoard(board), rackTiles(match.rack()), match.solutionCount().toInt())
  }

  /** Como se cerro la ultima mano; null si no hay mano cerrada. */
  private fun result(board: Board?): HandResult? {
    val outcome = outcomeOf(match.outcome()) ?: return null
    return HandResult(outcome, match.answer()?.let { bingo(board, it) }, bingos(board))
  }

  private fun bingos(board: Board?): List<Bingo> =
    (0 until match.solutionCount()).mapNotNull { match.solutionAt(it)?.let { s -> bingo(board, s) } }

  /** Con el tablero de la mano (si lo hay) para escribir la palabra entera. */
  private fun bingo(board: Board?, s: Solution): Bingo {
    val placement = "${s.coords} ${s.tiles}"
    return Bingo(placement, s.score.toInt(), board?.let { moveText(it, placement) } ?: plainTiles(placement))
  }

  private fun outcomeOf(text: String): HandOutcome? = when (text) {
    Sprint.OutcomeSolved -> HandOutcome.SOLVED
    Sprint.OutcomeTimeout -> HandOutcome.TIMEOUT
    Sprint.OutcomeGaveUp -> HandOutcome.GAVE_UP
    Sprint.OutcomeInvalid -> HandOutcome.INVALID
    else -> null
  }
}
