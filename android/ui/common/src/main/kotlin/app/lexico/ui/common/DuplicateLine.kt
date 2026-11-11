package app.lexico.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp

data class DuplicateLine(val players: Int, val round: Int, val leader: String, val country: String, val tied: Int, val efficiency: Int)

@Composable
internal fun RowScope.DuplicateRowContent(line: DuplicateLine) {
  val colors = MaterialTheme.colorScheme
  Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
    Text(summary(line, colors.primary), maxLines = 1, softWrap = false)
    if (line.round > 0 && line.tied <= 1) LeaderName(line, colors.primary)
  }
  Text(if (line.round > 0) "${line.efficiency} %" else "–", color = colors.tertiary, fontWeight = FontWeight.Bold)
}

private fun summary(line: DuplicateLine, accent: Color) = buildAnnotatedString {
  val bold = SpanStyle(fontWeight = FontWeight.Bold)
  if (line.round == 0) {
    withStyle(bold) { append("Ronda 1") }
    return@buildAnnotatedString append("  ·  ${line.players} jugadores")
  }
  withStyle(bold) { append("Ronda ${line.round}") }
  append("  ·  ")
  if (line.tied > 1) return@buildAnnotatedString withStyle(SpanStyle(color = accent, fontWeight = FontWeight.Bold)) { append("${line.tied} empatados") }
  append("gana ")
}

@Composable
private fun LeaderName(line: DuplicateLine, accent: Color) {
  Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
    Text(line.leader, Modifier.weight(1f, fill = false), color = accent, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    Text(flagOf(line.country), maxLines = 1, softWrap = false)
  }
}
