package app.lexico.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.lexico.model.Placement

/**
 * Una jugada de una lista del motor: "H8 CASA" o "(Pasar)", sus puntos, su valoracion (si la
 * hay), la colocacion para dibujarla y quien la jugo ("Tú", "Máster"...), si alguien.
 */
data class RankedMove(
  val text: String,
  val points: Int,
  val equity: Double? = null,
  val placement: Placement? = null,
  val playedBy: String = "",
)

/** Las jugadas en orden: puesto, jugada, puntos, valoracion y quien la jugo. Tocar una la elige. */
@Composable
fun RankedMoveList(moves: List<RankedMove>, selected: RankedMove?, modifier: Modifier = Modifier, select: (RankedMove) -> Unit) {
  LazyColumn(modifier) {
    itemsIndexed(moves) { n, m -> RankedMoveRow(n + 1, m, active = m == selected) { select(m) } }
  }
}

@Composable
private fun RankedMoveRow(rank: Int, m: RankedMove, active: Boolean, select: () -> Unit) {
  Row(Modifier.fillMaxWidth().highlight(active).clickable(onClick = select).padding(vertical = 2.dp, horizontal = 4.dp)) {
    Monospace("%2d".format(rank))
    Monospace(m.text, Modifier.weight(1f).padding(start = 8.dp))
    Monospace("${m.points}", Modifier.padding(horizontal = 6.dp))
    m.equity?.let { Monospace("%.1f".format(it)) }
    if (m.playedBy.isNotEmpty()) PlayedBy(m.playedBy)
  }
}

@Composable
private fun PlayedBy(who: String) {
  Text(who, Modifier.padding(start = 6.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
}

@Composable
private fun Modifier.highlight(active: Boolean): Modifier =
  background(if (active) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent, RoundedCornerShape(4.dp))

@Composable
private fun Monospace(text: String, modifier: Modifier = Modifier) {
  Text(text, modifier, fontFamily = FontFamily.Monospace, fontSize = 13.sp)
}
