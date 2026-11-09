package app.lexico.ui.common

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle

data class DuplicateLine(val players: Int, val round: Int, val leader: String, val country: String, val tied: Int, val efficiency: Int)

@Composable
internal fun RowScope.DuplicateRowContent(line: DuplicateLine) {
  val colors = MaterialTheme.colorScheme
  Text(summary(line, colors.primary), Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
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
  withStyle(SpanStyle(color = accent, fontWeight = FontWeight.Bold)) { append(line.leader) }
  append(" ${flagOf(line.country)}")
}
