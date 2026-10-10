package app.lexico.ui.games

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.lexico.ui.board.BoardStyle
import app.lexico.ui.board.RackTile
import app.lexico.ui.board.ScrabbleBoard
import app.lexico.ui.classic.OpponentPhoto
import app.lexico.ui.classic.PlayerBar
import app.lexico.ui.common.RankedMove

private const val FULL_RACK = 7
private val RACK_TILE = 42.dp
private val RACK_GAP = 4.dp
private val RACK_FRAME = 8.dp
private val NAV_TEXT = TextAutoSize.StepBased(minFontSize = 10.sp, maxFontSize = 14.sp, stepSize = 0.5.sp)

@Composable
internal fun TurnPage(review: ReviewView, index: Int, style: BoardStyle, height: Dp, go: (Int) -> Unit) {
  val turn = review.turns[index]
  var selected by remember(turn) { mutableStateOf<RankedMove?>(null) }
  var listing by remember { mutableStateOf(false) }
  val select = { m: RankedMove -> selected = if (m == selected) null else m }
  BoardFit(
    height,
    top = { Stacked { TurnTitle(turn, review.turns.size); OpponentBar(review, turn) } },
    board = { ScrabbleBoard(turn.board, Modifier, style, pending = selected?.placement?.placed.orEmpty()) },
    bottom = { Stacked { MyArea(turn, style); TurnNavigator(index, review.turns.size, go) { listing = true }; TurnMoves(turn, review.classic, selected, select) } },
  )
  if (listing) CandidatesDialog(turn.candidates, selected, close = { listing = false }) { select(it); listing = false }
}

@Composable
private fun Stacked(content: @Composable () -> Unit) {
  Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) { content() }
}

@Composable
private fun MyArea(turn: ReviewTurnView, style: BoardStyle) {
  PlayerBar(name = "Tú", points = turn.myScore, clock = null, onTurn = turn.mine == true, framed = true)
  ReviewRack(turn.rack, style)
}

@Composable
private fun TurnTitle(turn: ReviewTurnView, count: Int) {
  Text("Turno ${turn.number}/$count" + if (turn.player.isNotEmpty()) " · ${turn.player}" else "", fontWeight = FontWeight.Bold, maxLines = 1,
    overflow = TextOverflow.Ellipsis)
}

@Composable
private fun OpponentBar(review: ReviewView, turn: ReviewTurnView) {
  val photo: (@Composable () -> Unit)? = if (review.classic) ({ OpponentPhoto(review.opponent, 20.dp) }) else null
  PlayerBar(name = review.opponent, points = turn.opponentScore, clock = null, onTurn = turn.mine == false, framed = true, photo = photo)
}

@Composable
private fun TurnNavigator(index: Int, count: Int, go: (Int) -> Unit, list: () -> Unit) {
  Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(4.dp), Alignment.CenterVertically) {
    OutlinedButton(onClick = { go(index - 1) }, enabled = index > 0) { Text("‹") }
    TextButton(onClick = list, Modifier.weight(1f)) { Text("Ver lista de jugadas", maxLines = 1, autoSize = NAV_TEXT) }
    OutlinedButton(onClick = { go(index + 1) }, enabled = index < count - 1) { Text("›") }
  }
}

@Composable
private fun ReviewRack(rack: List<String>, style: BoardStyle) {
  BoxWithConstraints(Modifier.fillMaxWidth()) {
    val size = minOf(RACK_TILE, (maxWidth - RACK_FRAME - RACK_GAP * (FULL_RACK - 1)) / FULL_RACK)
    TileTray(rack, style, size)
  }
}

@Composable
private fun TileTray(rack: List<String>, style: BoardStyle, size: Dp) {
  val tray = style.tray?.let { Modifier.background(it, RoundedCornerShape(8.dp)).padding(4.dp) } ?: Modifier
  Row(Modifier.fillMaxWidth().then(tray), Arrangement.spacedBy(RACK_GAP, Alignment.CenterHorizontally)) {
    rack.forEach { RackTile(it, style, size) }
  }
}
