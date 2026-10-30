package app.lexico.ui.duplicate

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
private fun Cell(text: String, modifier: Modifier, bold: Boolean = false, end: Boolean = false, color: Color = Color.Unspecified) {
  Text(
    text, modifier, fontSize = 13.sp, fontWeight = if (bold) FontWeight.Bold else null, color = color,
    textAlign = if (end) TextAlign.End else TextAlign.Start, maxLines = 1, overflow = TextOverflow.Ellipsis,
  )
}
