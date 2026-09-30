package app.lexico.ui.classic

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.lexico.ui.common.MoveText
import app.lexico.ui.common.moveNumberWidth
import app.lexico.ui.common.MovesTableDialog

private typealias Turn = Pair<Move?, Move?>

@Composable
fun MovesDialog(view: ClassicView, close: () -> Unit) {
  val turns = remember(view.moves) { byTurn(view.moves) }
  val footer = view.end?.takeIf { it.ending != null || it.timePenalized }?.let { e -> @Composable { EndingRows(view, e) } }
  MovesTableDialog(turns, header = { MovesHeader(view.opponent) }, close = close, footer = footer) { i, turn -> TurnRow(i + 1, turn) }
}

private fun byTurn(moves: List<Move>): List<Turn> = moves.fold(mutableListOf()) { turns, m ->
  val last = turns.lastOrNull()
  when {
    m.side == Side.ME -> turns += m to null
    last == null || last.second != null -> turns += null to m
    else -> turns[turns.lastIndex] = last.first to m
  }
  turns
}

@Composable
private fun MovesHeader(opponent: String) {
  Row(Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
    Text("#", Modifier.width(moveNumberWidth()), fontSize = 11.sp, fontWeight = FontWeight.Bold)
    Text("Tú", Modifier.weight(1f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
    Separator()
    Text(opponent, Modifier.weight(1f), fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
  }
}

@Composable
private fun TurnRow(number: Int, turn: Turn) {
  Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
    Text("$number", Modifier.width(moveNumberWidth()), fontSize = 11.sp)
    MoveCell(turn.first)
    Separator()
    MoveCell(turn.second)
  }
}

@Composable
private fun EndingRows(view: ClassicView, end: GameEnd) {
  Column(Modifier.padding(top = 4.dp)) {
    DottedLine()
    if (end.timePenalized) DeltaRow("tiempo", -end.myTimePenalty, -end.opponentTimePenalty)
    end.ending?.let { DeltaRow("descuento", it.myDelta, it.opponentDelta) }
    EndingRow({ LabelCell("final") { TotalBadge(view.myScore) } }, { LabelCell("final") { TotalBadge(view.opponentScore) } })
  }
}

@Composable
private fun DeltaRow(label: String, mine: Int, theirs: Int) {
  EndingRow({ LabelCell(label) { DeltaText(mine) } }, { LabelCell(label) { DeltaText(theirs) } })
}

@Composable
private fun EndingRow(mine: @Composable RowScope.() -> Unit, theirs: @Composable RowScope.() -> Unit) {
  Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
    Spacer(Modifier.width(moveNumberWidth()))
    mine()
    Separator()
    theirs()
  }
}

@Composable
private fun RowScope.LabelCell(label: String, value: @Composable () -> Unit) {
  Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
    MoveText(label, Modifier.weight(1f))
    value()
  }
}

@Composable
private fun DeltaText(delta: Int) {
  Text(signed(delta), Modifier.padding(horizontal = 3.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold)
}

@Composable
private fun DottedLine() {
  val color = MaterialTheme.colorScheme.outline
  Canvas(Modifier.fillMaxWidth().height(9.dp)) {
    val dash = 4.dp.toPx()
    drawLine(
      color, Offset(0f, center.y), Offset(size.width, center.y), strokeWidth = 1.dp.toPx(),
      pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash, dash)),
    )
  }
}

@Composable
private fun Separator() {
  Text("|", Modifier.padding(horizontal = 3.dp), color = MaterialTheme.colorScheme.outline, fontSize = 12.sp)
}

@Composable
private fun RowScope.MoveCell(m: Move?) {
  Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
    if (m == null) return@Row
    MoveText(moveText(m), Modifier.weight(1f))
    Text("${m.points}", Modifier.padding(horizontal = 3.dp), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    TotalBadge(if (m.side == Side.ME) m.myTotal else m.opponentTotal)
  }
}

private fun moveText(m: Move): String = when (m.type) {
  MoveType.PLACEMENT -> m.text
  MoveType.INVALID -> "inválida"
  MoveType.PASS -> "pase"
  MoveType.EXCHANGE -> "cambio " + m.text.ifEmpty { "${m.tiles}" }
}

@Composable
private fun TotalBadge(total: Int) {
  Text(
    "$total", Modifier.background(MaterialTheme.colorScheme.primary, RoundedCornerShape(6.dp)).padding(horizontal = 5.dp, vertical = 1.dp),
    color = MaterialTheme.colorScheme.onPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold,
  )
}
