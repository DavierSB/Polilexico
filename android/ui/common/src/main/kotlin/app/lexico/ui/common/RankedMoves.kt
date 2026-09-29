package app.lexico.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
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

/**
 * Las jugadas en una tabla: puesto, jugada (con quien la jugo), puntos y, si las hay, equity; la
 * cabecera de equity explica que es. Tocar una fila la elige.
 */
@Composable
fun RankedMoveList(moves: List<RankedMove>, selected: RankedMove?, modifier: Modifier = Modifier, select: (RankedMove) -> Unit) {
  val withEquity = moves.any { it.equity != null }
  var explaining by remember { mutableStateOf(false) }
  Column(modifier) {
    TableHeader(withEquity) { explaining = true }
    HorizontalDivider()
    LazyColumn(Modifier.weight(1f)) {
      itemsIndexed(moves) { n, m -> MoveRow(n + 1, m, withEquity, active = m == selected) { select(m) } }
    }
  }
  if (explaining) InfoDialog("Equity", EQUITY) { explaining = false }
}

private val RankWidth = 28.dp
private val PointsWidth = 56.dp
private val EquityWidth = 76.dp

@Composable
private fun TableHeader(withEquity: Boolean, explain: () -> Unit) {
  Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
    HeaderCell("#", Modifier.width(RankWidth))
    HeaderCell("Jugada", Modifier.weight(1f))
    HeaderCell("Puntos", Modifier.width(PointsWidth), TextAlign.End)
    if (withEquity) EquityHeader(explain)
  }
}

/** "Equity" con su boton de informacion. */
@Composable
private fun EquityHeader(explain: () -> Unit) {
  Row(Modifier.width(EquityWidth).clickable(onClick = explain), Arrangement.End, Alignment.CenterVertically) {
    HeaderCell("Equity")
    InfoIcon(Modifier.padding(start = 4.dp))
  }
}

@Composable
private fun HeaderCell(text: String, modifier: Modifier = Modifier, align: TextAlign = TextAlign.Start) {
  Text(text.uppercase(), modifier, style = headerStyle(), textAlign = align)
}

@Composable
private fun MoveRow(rank: Int, m: RankedMove, withEquity: Boolean, active: Boolean, select: () -> Unit) {
  Row(
    Modifier.fillMaxWidth().padding(vertical = 1.dp).highlight(active).clickable(onClick = select).padding(horizontal = 8.dp, vertical = 7.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Cell("$rank", RankWidth, cellStyle(FontWeight.SemiBold, muted = true))
    MoveCell(m)
    Cell("${m.points}", PointsWidth, cellStyle(FontWeight.ExtraBold), TextAlign.End)
    if (withEquity) Cell(m.equity?.let { "%.1f".format(it) }.orEmpty(), EquityWidth, cellStyle(FontWeight.SemiBold, muted = true), TextAlign.End)
  }
}

/** La jugada, que se lleva el ancho sobrante, y quien la jugo. */
@Composable
private fun RowScope.MoveCell(m: RankedMove) {
  Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
    Text(m.text, style = cellStyle(FontWeight.Bold).copy(letterSpacing = 0.6.sp))
    if (m.playedBy.isNotEmpty()) PlayedBy(m.playedBy)
  }
}

@Composable
private fun Cell(text: String, width: Dp, style: TextStyle, align: TextAlign = TextAlign.Start) {
  Text(text, Modifier.width(width), style = style, textAlign = align)
}

@Composable
private fun PlayedBy(who: String) {
  Text(
    who, Modifier.padding(start = 8.dp).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(50)).padding(horizontal = 7.dp),
    style = TextStyle(fontFamily = Mulish, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimary),
  )
}

private const val EQUITY = "El valor de la jugada, considerando su puntuación y lo que queda en la mano."

@Composable
private fun headerStyle(): TextStyle = TextStyle(
  fontFamily = Mulish, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp, letterSpacing = 1.2.sp,
  color = MaterialTheme.colorScheme.primary,
)

@Composable
private fun cellStyle(weight: FontWeight, muted: Boolean = false): TextStyle = TextStyle(
  fontFamily = Mulish, fontWeight = weight, fontSize = 15.sp,
  color = if (muted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
)

@Composable
private fun Modifier.highlight(active: Boolean): Modifier =
  background(if (active) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent, RoundedCornerShape(10.dp))
