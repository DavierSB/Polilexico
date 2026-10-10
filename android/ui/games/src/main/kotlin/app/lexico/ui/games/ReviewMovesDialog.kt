package app.lexico.ui.games

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.lexico.ui.common.MoveText
import app.lexico.ui.common.MovesTableDialog
import app.lexico.ui.common.moveNumberWidth

@Composable
internal fun ReviewMovesDialog(table: MovesTable, close: () -> Unit, go: (Int) -> Unit) {
  MovesTableDialog(table.rows, header = { MovesHeader(table) }, close = close) { i, row -> MovesLine(i + 1, row) { go(row.turn) } }
}

@Composable
private fun MovesHeader(table: MovesTable) {
  Row(Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
    Text("#", Modifier.width(moveNumberWidth()), fontSize = 11.sp, fontWeight = FontWeight.Bold)
    HeaderName(table.left)
    Separator()
    HeaderName(table.right)
  }
}

@Composable
private fun RowScope.HeaderName(name: String) {
  Text(name, Modifier.weight(1f), fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
}

@Composable
private fun MovesLine(number: Int, row: MovesRow, open: () -> Unit) {
  Row(Modifier.fillMaxWidth().clickable(onClick = open).padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
    Text("$number", Modifier.width(moveNumberWidth()), fontSize = 11.sp)
    MoveCell(row.left)
    Separator()
    MoveCell(row.right)
  }
}

@Composable
private fun RowScope.MoveCell(m: PlayedMove?) {
  Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
    if (m == null) return@Row
    MoveText(m.text, Modifier.weight(1f))
    Text("${m.points}", Modifier.padding(horizontal = 3.dp), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
    TotalBadge(m.total)
  }
}

@Composable
private fun Separator() {
  Text("|", Modifier.padding(horizontal = 3.dp), color = MaterialTheme.colorScheme.outline, fontSize = 12.sp)
}

@Composable
private fun TotalBadge(total: Int) {
  Text(
    "$total", Modifier.background(MaterialTheme.colorScheme.primary, RoundedCornerShape(6.dp)).padding(horizontal = 5.dp, vertical = 1.dp),
    color = MaterialTheme.colorScheme.onPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1,
  )
}
