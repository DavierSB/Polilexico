package app.lexico.ui.duplicate

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.lexico.ui.common.SeatAvatar

@Composable
internal fun StandingsTable(view: SeatView, maxHeight: Int = 220) {
  Column(Modifier.fillMaxWidth().heightIn(max = maxHeight.dp).verticalScroll(rememberScrollState())) {
    HeaderRow()
    view.standings.forEachIndexed { i, e -> EntryRow(placeOf(view.standings, i), e, mine = e.name == view.me) }
  }
}

private fun placeOf(sorted: List<SeatEntry>, i: Int): Int = sorted.indexOfFirst { it.total == sorted[i].total } + 1

@Composable
private fun HeaderRow() {
  Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
    Cell("#", Modifier.width(22.dp), bold = true)
    Cell("Jugador", Modifier.weight(1f), bold = true)
    Cell("Jugada", Modifier.weight(1.4f), bold = true)
    Cell("Pts", Modifier.width(36.dp), bold = true, end = true)
    Cell("Total", Modifier.width(44.dp), bold = true, end = true)
  }
}

@Composable
private fun EntryRow(place: Int, e: SeatEntry, mine: Boolean) {
  val color = if (mine) MaterialTheme.colorScheme.primary else Color.Unspecified
  Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
    Cell("$place", Modifier.width(22.dp), color = color)
    Cell(e.name, Modifier.weight(1f), bold = mine || e.name == MASTER_NAME, color = color)
    Cell(e.text, Modifier.weight(1.4f), color = color)
    Cell("${e.points}", Modifier.width(36.dp), end = true, color = color)
    Cell("${e.total}", Modifier.width(44.dp), bold = true, end = true, color = color)
  }
}

@Composable
internal fun LeaderboardTable(view: SeatView, maxHeight: Int = 260) {
  val master = view.standings.find { it.name == MASTER_NAME }?.total ?: 0
  val known = view.standings.filter { it.name != MASTER_NAME }
  val rows = known.ifEmpty { view.players.map { SeatEntry(it, "", -1, -1) } }
  Column(
    Modifier.fillMaxWidth().heightIn(max = maxHeight.dp).verticalScroll(rememberScrollState()),
    verticalArrangement = Arrangement.spacedBy(6.dp),
  ) { rows.forEachIndexed { i, e -> LeaderCard(placeOf(rows, i), e, master) } }
}

@Composable
private fun LeaderCard(place: Int, e: SeatEntry, master: Int) {
  val colors = MaterialTheme.colorScheme
  Row(
    Modifier.fillMaxWidth().background(if (place == 1 && e.total > 0) colors.primaryContainer else colors.surfaceVariant, RoundedCornerShape(14.dp))
      .padding(horizontal = 10.dp, vertical = 8.dp),
    horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically,
  ) {
    Text("$place", fontWeight = FontWeight.Bold, modifier = Modifier.width(18.dp))
    SeatAvatar(e.name, 32.dp)
    Column(Modifier.weight(1f)) {
      Text(e.name, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
      Text("Ronda ${roundPoints(e.points)}", style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
    }
    Column(horizontalAlignment = Alignment.End) {
      Text(e.total.takeIf { it >= 0 }?.toString() ?: UNKNOWN, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = colors.primary)
      Text(efficiency(e.total, master), style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
    }
  }
}

private fun roundPoints(points: Int): String = if (points < 0) UNKNOWN else "+$points"

private fun efficiency(total: Int, master: Int): String =
  if (total < 0 || master <= 0) UNKNOWN else "%.1f %%".format(total * 100f / master)

private const val UNKNOWN = "—"

@Composable
private fun Cell(text: String, modifier: Modifier, bold: Boolean = false, end: Boolean = false, color: Color = Color.Unspecified) {
  Text(
    text, modifier, fontSize = 13.sp, fontWeight = if (bold) FontWeight.Bold else null, color = color,
    textAlign = if (end) TextAlign.End else TextAlign.Start, maxLines = 1, overflow = TextOverflow.Ellipsis,
  )
}
