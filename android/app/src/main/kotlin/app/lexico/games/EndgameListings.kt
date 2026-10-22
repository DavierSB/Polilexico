package app.lexico.games

import app.lexico.game.records.EndgameExplorer
import app.lexico.game.records.EndgameLine
import app.lexico.game.records.EndgameReview
import app.lexico.game.records.ExplorerPosition
import app.lexico.game.records.LineEnding
import app.lexico.game.records.LineStep
import app.lexico.game.records.ReviewMove
import app.lexico.ui.common.RankedMove
import app.lexico.ui.games.EndgameSession
import app.lexico.ui.games.EndingRow
import app.lexico.ui.games.ExploredPosition
import app.lexico.ui.games.LineView
import app.lexico.ui.games.LiveLines
import app.lexico.ui.games.ReviewTurnView
import app.lexico.ui.games.StepView

private const val ME = "Tú"

fun endgameSession(explorer: EndgameExplorer, turn: ReviewTurnView, opponent: String): EndgameSession =
  ExplorerSession(explorer, if (turn.mine == false) listOf(opponent, ME) else listOf(ME, opponent))

private class ExplorerSession(private val explorer: EndgameExplorer, private val players: List<String>) : EndgameSession {
  override suspend fun position(): ExploredPosition = positionView(explorer.position(), players)

  override suspend fun play(move: String?): String? = explorer.play(move)

  override suspend fun back(steps: Int) = explorer.back(steps)

  override suspend fun analyze(update: (LiveLines) -> Unit) {
    val mover = explorer.position().onTurn
    explorer.analyze { update(liveLines(it, players, mover)) }
  }
}

private fun positionView(p: ExplorerPosition, players: List<String>): ExploredPosition =
  ExploredPosition(p.board, players, p.racks, p.scores, p.onTurn, p.over, steps(p.path, players, 0), endingRows(p.ending, players))

private fun liveLines(r: EndgameReview, players: List<String>, mover: Int): LiveLines =
  LiveLines(r.lines.map { lineView(it, players, mover) }, r.depth, r.exact)

private fun lineView(l: EndgameLine, players: List<String>, mover: Int): LineView =
  LineView(
    ranked(l.move, l.spread.takeUnless { l.pending }), l.spread, l.pending, players[mover], steps(l.steps, players, mover),
    endingRows(l.ending, players),
  )

private fun steps(steps: List<LineStep>, players: List<String>, first: Int): List<StepView> =
  steps.mapIndexed { i, s -> StepView(players[(first + i) % 2], ranked(s.move, null), s.board) }

private fun endingRows(e: LineEnding, players: List<String>): List<EndingRow> =
  listOf(EndingRow(players[0], e.moverLeft, e.moverDelta), EndingRow(players[1], e.otherLeft, e.otherDelta))

private fun ranked(m: ReviewMove, final: Int?): RankedMove = RankedMove(m.text, m.score, null, m.placement, final = final)
