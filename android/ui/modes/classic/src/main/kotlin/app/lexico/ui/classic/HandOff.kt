package app.lexico.ui.classic

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.lexico.ui.common.Covered

@Composable
internal fun HandOff(view: ClassicView, content: @Composable () -> Unit) {
  var shownTo by remember { mutableStateOf<String?>(null) }
  val covered = view.handOff && view.end == null && shownTo != view.me
  Covered(covered, alpha = 1f, content) { HandOffCover(view) { shownTo = view.me } }
}

@Composable
private fun ColumnScope.HandOffCover(view: ClassicView, onReady: () -> Unit) {
  Relay(view.opponent, view.me)
  HandOffTitle(view.me)
  ScoreChips(view)
  if (view.moves.isNotEmpty()) MovesCard(view, Modifier.fillMaxWidth().weight(1f, fill = false))
  Button(onClick = onReady, Modifier.fillMaxWidth()) { Text("Soy ${view.me}", fontSize = 16.sp, fontWeight = FontWeight.Bold) }
}

@Composable
private fun Relay(from: String, to: String) {
  Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
    Initial(from, 52.dp, MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.outline)
    Text("• • ›", color = MaterialTheme.colorScheme.primary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    Box(Modifier.border(6.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), CircleShape).padding(6.dp)) {
      Initial(to, 72.dp, MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.onPrimary)
    }
  }
}

@Composable
private fun Initial(name: String, size: Dp, background: Color, ink: Color) {
  Box(Modifier.size(size).background(background, CircleShape), contentAlignment = Alignment.Center) {
    Text(initialOf(name), color = ink, fontSize = (size.value * 0.45f).sp, fontWeight = FontWeight.Black)
  }
}

private fun initialOf(name: String): String = name.trim().take(1).uppercase().ifEmpty { "?" }

@Composable
private fun HandOffTitle(name: String) {
  val primary = MaterialTheme.colorScheme.primary
  val text = buildAnnotatedString {
    append("Pásale el teléfono a ")
    withStyle(SpanStyle(color = primary)) { append(name) }
  }
  Text(text, fontSize = 30.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center, lineHeight = 35.sp)
}

@Composable
private fun ScoreChips(view: ClassicView) {
  FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally), verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Chip(view.me, view.myScore.toString())
    Chip(view.opponent, view.opponentScore.toString())
    Chip("Turno", (view.moves.size + 1).toString())
  }
}

@Composable
private fun Chip(label: String, value: String) {
  val text = buildAnnotatedString {
    append("$label ")
    withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)) { append(value) }
  }
  Text(text, Modifier.background(MaterialTheme.colorScheme.surfaceVariant, CircleShape).padding(horizontal = 12.dp, vertical = 6.dp), maxLines = 1)
}

@Composable
private fun MovesCard(view: ClassicView, modifier: Modifier) {
  Box(modifier.background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(14.dp)).padding(10.dp)) {
    MovesList(view, Modifier.fillMaxWidth())
  }
}
