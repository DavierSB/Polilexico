package app.lexico.game.modes.classic

import app.lexico.game.Outcome
import app.lexico.game.engine.parseBoard
import app.lexico.game.engine.placedSquares
import app.lexico.game.engine.rackTiles
import app.lexico.go.classic.Classic
import app.lexico.go.classic.Clocks
import app.lexico.go.classic.Game
import app.lexico.go.classic.Match
import app.lexico.go.classic.Move
import app.lexico.go.classic.Result
import app.lexico.model.Position

/** Lee del motor el estado de una partida clasica en marcha y lo traduce a [ClassicState]. */
internal class ClassicReader(private val match: Match) {
  fun read(): ClassicState {
    val game = match.game()
    val status = game.status()
    val moves = moves(game)
    return ClassicState(
      board = parseBoard(game.board(), latestSquares(moves)), rack = rackTiles(game.rack()),
      opponentTiles = status.opponentTiles.toInt(), opponentRack = rackTiles(game.opponentRack()),
      myScore = status.humanScore.toInt(), opponentScore = status.botScore.toInt(), myTurn = status.humanToMove,
      bag = status.bagCount.toInt(), unseen = rackTiles(game.unseen()),
      moves = moves, clocks = clocks(),
      paused = match.paused(), result = game.result()?.let(::result), botError = match.botError().ifEmpty { null },
    )
  }

  fun clocks(): ClassicClocks? = match.clocks()?.let(::clocks)

  private fun moves(game: Game): List<PlayedMove> = (0 until game.moveCount()).map { playedMove(game.moveAt(it)) }

  private fun playedMove(m: Move): PlayedMove = PlayedMove(
    byMe = m.byHuman, kind = moveKind(m.kind), coords = m.coords, tiles = m.tiles, tileCount = m.tileCount.toInt(),
    score = m.score.toInt(), myTotal = m.humanTotal.toInt(), opponentTotal = m.botTotal.toInt(),
  )

  private fun clocks(c: Clocks): ClassicClocks =
    ClassicClocks(c.humanMs, c.humanOvertimeMs, c.botMs, c.botOvertimeMs, running(c.running))

  private fun result(r: Result): ClassicResult = ClassicResult(Outcome.of(r.outcome), r.lostOnTime, r.recordPath)

  /** Las fichas de la ultima jugada, para resaltarlas. */
  private fun latestSquares(moves: List<PlayedMove>): Set<Position> =
    moves.lastOrNull()?.takeIf { it.kind == MoveKind.PLACEMENT }?.let { placedSquares(it.coords, it.tiles) }.orEmpty()

  private fun moveKind(kind: String): MoveKind = when (kind) {
    "play" -> MoveKind.PLACEMENT
    "exchange" -> MoveKind.EXCHANGE
    "invalid" -> MoveKind.INVALID
    else -> MoveKind.PASS
  }

  private fun running(side: String): Side? = when (side) {
    Classic.RunningHuman -> Side.ME
    Classic.RunningBot -> Side.OPPONENT
    else -> null
  }

}
