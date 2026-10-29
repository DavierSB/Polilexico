package app.lexico.ui.games

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.lexico.ui.board.BoardStyle
import app.lexico.ui.board.WordTap
import app.lexico.ui.board.PlacingBoard
import app.lexico.ui.board.PlacingRack
import app.lexico.ui.board.RackRenewal
import app.lexico.ui.board.ScrabbleBoard
import app.lexico.ui.board.TilePlacer
import app.lexico.ui.board.rememberTilePlacer
import app.lexico.ui.common.NOT_IN_A_LINE
import app.lexico.ui.common.Notice
import kotlinx.coroutines.delay

private const val NOTICE_MS = 3_000L
private val RACK_TILE = 42.dp
private val TIGHT = PaddingValues(horizontal = 2.dp)
private val BUTTON_TEXT = TextAutoSize.StepBased(minFontSize = 9.sp, maxFontSize = 14.sp, stepSize = 0.5.sp)

private enum class Shown { NOTHING, LIST, PLAN }

@Composable
internal fun EndgamePage(review: ReviewView, index: Int, sessions: EndgameSessions, style: BoardStyle, height: Dp, go: (Int) -> Unit) {
  val x = rememberExploration(sessions, index)
  val position = x.position ?: return OpeningPage(review, index, style, height, x.failed, go)
  val c = rememberTilePlacer(RackRenewal.WHOLE)
  var shown by remember { mutableStateOf(Shown.NOTHING) }
  var planned by remember(position) { mutableStateOf<String?>(null) }
  LaunchedEffect(position) { c.reset(position.board, if (position.over) emptyList() else position.racks[position.onTurn]) }
  LaunchedEffect(x.notice) { if (x.notice != null) delay(NOTICE_MS).also { x.notice = null } }
  val plan = { l: LineView -> planned = l.move.text; shown = Shown.PLAN }
  BoardFit(
    height,
    top = { Stacked { ExploreTitle(review, index, position.path, go, x::back); SideBar(review, position, 1 - position.me, style) } },
    board = { PlacingBoard(c, style, enabled = !position.over, lookUpWhenIdle = true) },
    bottom = { Stacked { PlayerArea(review, index, position, c, style, x, plan) { shown = it } } },
  )
  Dialogs(review, position, shown, x, marked(x.lines, review.turns[index], position), planned, style, plan) { shown = Shown.NOTHING }
}

@Composable
private fun OpeningPage(review: ReviewView, index: Int, style: BoardStyle, height: Dp, failed: Boolean, go: (Int) -> Unit) {
  BoardFit(
    height,
    top = { ExploreTitle(review, index, emptyList(), go) {} },
    board = { ScrabbleBoard(review.turns[index].board, Modifier, style, wordTap = WordTap.MENU) },
    bottom = { LinesTable(emptyList(), AnalysisState(LiveLines.NONE, analyzing = !failed, failed), plan = {}) },
  )
}

@Composable
private fun PlayerArea(
  review: ReviewView, index: Int, position: ExploredPosition, c: TilePlacer, style: BoardStyle, x: Exploration, plan: (LineView) -> Unit,
  show: (Shown) -> Unit,
) {
  SideBar(review, position, position.me, style)
  if (!position.over) TurnRack(position, c, style)
  x.notice?.let { Notice(it) }
  if (!position.over) ExploreButtons(c, x, plan = { x.lines.lines.firstOrNull()?.let(plan) }) { show(Shown.LIST) }
  if (position.over) EndgameResult(position) else Summary(review.turns[index], marked(x.lines, review.turns[index], position), x, plan)
}

@Composable
private fun Dialogs(
  review: ReviewView, position: ExploredPosition, shown: Shown, x: Exploration, lines: LiveLines, planned: String?, style: BoardStyle,
  plan: (LineView) -> Unit, close: () -> Unit,
) {
  val line = lines.lines.find { it.move.text == planned }
  when {
    shown == Shown.LIST -> LinesDialog(lines, x.analyzing, close, plan)
    shown == Shown.PLAN && line != null -> PlanDialog(review, position, line, style, close)
  }
}

@Composable
private fun TurnRack(position: ExploredPosition, c: TilePlacer, style: BoardStyle) {
  if (position.onTurn != position.me) Text("Juega ${position.players[position.onTurn]}".uppercase(), style = captionStyle())
  PlacingRack(c, style, enabled = true, size = RACK_TILE)
}

@Composable
private fun SideBar(review: ReviewView, position: ExploredPosition, player: Int, style: BoardStyle) {
  val onTurn = !position.over && position.onTurn == player
  ScoreBar(review, position.players[player], position.scores[player], position.racks[player].takeUnless { onTurn }, onTurn, style)
}

@Composable
private fun ExploreButtons(c: TilePlacer, x: Exploration, plan: () -> Unit, list: () -> Unit) {
  val play = { c.placement()?.let { x.play(it.toString()) } ?: run { x.notice = NOT_IN_A_LINE } }
  val ready = x.lines.lines.isNotEmpty()
  Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
    Button(modifier = Modifier.weight(1f), contentPadding = TIGHT, enabled = c.hasPlaced, onClick = { play() }) { Label("Jugar") }
    FilledTonalButton(modifier = Modifier.weight(1f), contentPadding = TIGHT, onClick = { if (c.hasPlaced) c.recall() else c.shuffle() }) {
      Label(if (c.hasPlaced) "Recoger" else "Mezclar")
    }
    OutlinedButton(modifier = Modifier.weight(1f), contentPadding = TIGHT, onClick = { x.play(null) }) { Label("Pasar") }
    OutlinedButton(modifier = Modifier.weight(1f), contentPadding = TIGHT, enabled = ready, onClick = plan) { Label("Ver plan") }
    OutlinedButton(modifier = Modifier.weight(1f), contentPadding = TIGHT, enabled = ready, onClick = list) { Label("Ver lista") }
  }
}

@Composable
private fun Label(text: String) {
  Text(text, maxLines = 1, softWrap = false, autoSize = BUTTON_TEXT)
}

private fun marked(lines: LiveLines, turn: ReviewTurnView, position: ExploredPosition): LiveLines {
  if (position.path.isNotEmpty()) return lines
  return lines.copy(lines = lines.lines.map { l -> l.copy(move = l.move.copy(playedBy = playedBy(turn, l))) })
}

private fun playedBy(turn: ReviewTurnView, line: LineView): String =
  turn.marks.filter { it.move.text == line.move.text }.joinToString(" ") { it.who }
