package app.lexico.ui.classic

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.lexico.ui.common.Durations

private val OvertimeRed = Color(0xFFB3261E)
private val OvertimePink = Color(0xFFF2B8B5)

@Composable
fun PlayerBar(
  name: String,
  points: Int,
  clock: Clock?,
  onTurn: Boolean,
  thinking: Boolean = false,
  framed: Boolean = false,
  photo: (@Composable () -> Unit)? = null,
  tiles: @Composable () -> Unit = {},
) {
  Row(Modifier.fillMaxWidth().turnBorder(onTurn, framed).padding(horizontal = 10.dp, vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
    Identity(name, thinking, photo, tiles)
    Text("$points", Modifier.padding(horizontal = 10.dp), fontSize = 22.sp, fontWeight = FontWeight.Bold)
    if (clock != null) PlayerClock(clock, onTurn)
  }
}

@Composable
private fun Modifier.turnBorder(onTurn: Boolean, framed: Boolean): Modifier {
  val color = if (onTurn || framed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
  return border(if (onTurn) 2.dp else 1.dp, color, RoundedCornerShape(10.dp))
}

@Composable
private fun RowScope.Identity(name: String, thinking: Boolean, photo: (@Composable () -> Unit)?, tiles: @Composable () -> Unit) {
  Column(Modifier.weight(1f)) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      photo?.let { it(); Spacer(Modifier.width(6.dp)) }
      Text(name, fontWeight = FontWeight.Bold)
      if (thinking) Text("  pensando…", style = MaterialTheme.typography.labelSmall)
    }
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) { tiles() }
  }
}

@Composable
private fun PlayerClock(clock: Clock, onTurn: Boolean) {
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Text(
      if (clock.inOvertime) "-" + Durations.format(-clock.remainingMs) else Durations.format(clock.remainingMs),
      Modifier.background(clockFill(clock, onTurn), RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 4.dp).width(58.dp),
      color = clockInk(clock, onTurn), fontFamily = FontFamily.Monospace, fontSize = 16.sp, textAlign = TextAlign.Center,
    )
    if (clock.inOvertime) Text("descuento", fontSize = 10.sp, color = OvertimePink)
  }
}

@Composable
private fun clockFill(clock: Clock, onTurn: Boolean): Color = when {
  clock.inOvertime -> OvertimeRed
  onTurn -> MaterialTheme.colorScheme.primary
  else -> MaterialTheme.colorScheme.surfaceVariant
}

@Composable
private fun clockInk(clock: Clock, onTurn: Boolean): Color = when {
  clock.inOvertime -> Color.White
  onTurn -> MaterialTheme.colorScheme.onPrimary
  else -> MaterialTheme.colorScheme.onSurfaceVariant
}
