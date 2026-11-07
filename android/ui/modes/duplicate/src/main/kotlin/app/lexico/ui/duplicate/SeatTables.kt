package app.lexico.ui.duplicate

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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

private const val UNKNOWN = "—"

private enum class CardTone { PLAIN, MINE, LEADER, MASTER }

@Composable
internal fun StandingsTable(view: SeatView, modifier: Modifier = Modifier.heightIn(max = 260.dp)) {
  Column(modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
    PlainHeader()
    view.standings.forEachIndexed { i, e -> PlainRow(placeOf(view.standings, i), e, mine = e.name == view.me) }
  }
}

@Composable
private fun PlainHeader() {
  Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
    Cell("#", Modifier.width(PLACE_W), bold = true)
    Cell("Jugador", Modifier.weight(1f), bold = true)
    Cell("Jugada", Modifier.weight(1.7f), bold = true)
    Cell("Pts", Modifier.width(POINTS_W), bold = true, end = true)
    Cell("Total", Modifier.width(TOTAL_W), bold = true, end = true)
  }
}

@Composable
private fun PlainRow(place: Int, e: SeatEntry, mine: Boolean) {
  val color = if (mine) MaterialTheme.colorScheme.primary else Color.Unspecified
  Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
    Cell("$place", Modifier.width(PLACE_W), color = color)
    Cell(e.name, Modifier.weight(1f), bold = mine || e.name == MASTER_NAME, color = color)
    Cell(e.text, Modifier.weight(1.7f), color = color, lines = 2)
    Cell("${e.points}", Modifier.width(POINTS_W), end = true, color = color)
    Cell("${e.total}", Modifier.width(TOTAL_W), bold = true, end = true, color = color)
  }
}

@Composable
private fun Cell(text: String, modifier: Modifier, bold: Boolean = false, end: Boolean = false, color: Color = Color.Unspecified, lines: Int = 1) {
  Text(
    text, modifier, fontSize = 13.sp, fontWeight = if (bold) FontWeight.Bold else null, color = color,
    textAlign = if (end) TextAlign.End else TextAlign.Start, maxLines = lines, overflow = TextOverflow.Ellipsis,
  )
}

private val PLACE_W = 20.dp
private val POINTS_W = 34.dp
private val TOTAL_W = 46.dp

@Composable
internal fun LeaderboardTable(view: SeatView, maxHeight: Int = 260) {
  val master = view.standings.find { it.name == MASTER_NAME }?.total ?: 0
  val known = view.standings.filter { it.name != MASTER_NAME }
  val rows = known.ifEmpty { view.players.map { SeatEntry(it, "", -1, -1) } }
  CardList(maxHeight) {
    rows.forEachIndexed { i, e ->
      val place = placeOf(rows, i)
      val tone = if (place == 1 && e.total > 0) CardTone.LEADER else CardTone.PLAIN
      EntryCard("$place", e.name, "Ronda ${roundPoints(e.points)}", known(e.total), efficiency(e.total, master), tone)
    }
  }
}

@Composable
private fun CardList(maxHeight: Int, content: @Composable () -> Unit) {
  Column(
    Modifier.fillMaxWidth().heightIn(max = maxHeight.dp).verticalScroll(rememberScrollState()),
    verticalArrangement = Arrangement.spacedBy(6.dp),
  ) { content() }
}

@Composable
private fun EntryCard(place: String, name: String, detail: String, total: String, extra: String?, tone: CardTone) {
  val colors = MaterialTheme.colorScheme
  val shape = RoundedCornerShape(14.dp)
  val fill = if (tone == CardTone.MINE || tone == CardTone.LEADER) colors.primaryContainer else colors.surfaceVariant
  val edge = if (tone == CardTone.MASTER) Modifier.border(1.5.dp, colors.primary, shape) else Modifier
  Row(
    Modifier.fillMaxWidth().then(edge).background(fill, shape).padding(horizontal = 10.dp, vertical = 8.dp),
    horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(place, fontWeight = FontWeight.Bold, modifier = Modifier.width(18.dp))
    if (tone != CardTone.MASTER) SeatAvatar(name, 32.dp)
    Column(Modifier.weight(1f)) {
      Text(name, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
      Text(detail, style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
    Column(horizontalAlignment = Alignment.End) {
      Text(total, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = colors.primary)
      extra?.let { Text(it, style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant) }
    }
  }
}

private fun placeOf(sorted: List<SeatEntry>, i: Int): Int = sorted.indexOfFirst { it.total == sorted[i].total } + 1

private fun roundPoints(points: Int): String = if (points < 0) UNKNOWN else "+$points"

private fun known(total: Int): String = total.takeIf { it >= 0 }?.toString() ?: UNKNOWN

private fun efficiency(total: Int, master: Int): String =
  if (total < 0 || master <= 0) UNKNOWN else "%.1f %%".format(total * 100f / master)
