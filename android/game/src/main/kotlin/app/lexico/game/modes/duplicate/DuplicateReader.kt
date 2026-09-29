package app.lexico.game.modes.duplicate

import app.lexico.game.engine.parseBoard
import app.lexico.game.engine.placedSquares
import app.lexico.game.engine.plainTiles
import app.lexico.game.engine.rackTiles
import app.lexico.go.duplicate.Duplicate
import app.lexico.go.duplicate.Game
import app.lexico.go.duplicate.Match
import app.lexico.go.duplicate.Turn
import app.lexico.model.Position

/** Lee del motor el estado de una duplicada en marcha y lo traduce a [DuplicateState]. */
internal class DuplicateReader(private val match: Match) {
  fun read(): DuplicateState {
    val game = match.game()
    val turns = turns(game)
    return DuplicateState(
      board = parseBoard(game.board(), latestSquares(turns)), phase = phase(), rounds = turns.map(::round),
      bag = rackTiles(game.unseen()), paused = match.paused(), lastError = match.lastError().ifEmpty { null },
      recordPath = game.result()?.recordPath,
    )
  }

  /** La fase, con el tiempo al dia. */
  fun phase(): DuplicatePhase = when (match.phase()) {
    Duplicate.PhaseInvalidRack -> DuplicatePhase.InvalidRack(rackTiles(match.invalidRack()))
    Duplicate.PhasePlaying -> DuplicatePhase.Playing(rackTiles(match.rack()), match.turnRemainingMs())
    Duplicate.PhaseConfirming -> confirming()
    Duplicate.PhaseFinished -> DuplicatePhase.Finished
    else -> DuplicatePhase.Waiting(match.turnMs())
  }

  private fun confirming(): DuplicatePhase {
    val proposal = match.proposal()
    return DuplicatePhase.Confirming(
      rackTiles(match.rack()), "${proposal.coords} ${plainTiles(proposal.tiles)} (${proposal.score})",
      match.turnRemainingMs(), match.cancelRemainingMs(),
    )
  }

  private fun turns(game: Game): List<Turn> = (0 until game.turnCount()).map { game.turnAt(it) }

  private fun round(t: Turn): RoundResult = RoundResult(
    number = t.number.toInt(), masterText = "${t.masterCoords} ${plainTiles(t.masterTiles)}", masterScore = t.masterScore.toInt(),
    myText = myText(t), myScore = t.humanScore.toInt(), hit = t.hit,
  )

  /** Tu jugada en palabras: la colocacion, "pase", "inválida" o "tiempo agotado". */
  private fun myText(t: Turn): String = when (t.humanKind) {
    "play" -> "${t.humanCoords} ${plainTiles(t.humanTiles)}"
    "invalid" -> "inválida"
    "timeout" -> "tiempo agotado"
    else -> "pase"
  }

  /** Las fichas de la ultima jugada del master, para resaltarlas. */
  private fun latestSquares(turns: List<Turn>): Set<Position> =
    turns.lastOrNull()?.let { placedSquares(it.masterCoords, it.masterTiles) }.orEmpty()
}
