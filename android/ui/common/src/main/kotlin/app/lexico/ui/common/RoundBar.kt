package app.lexico.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private const val LOW_ROUND_MS = 10_000L

@Composable
fun RoundBar(round: Int, remainingMs: Long?) {
  Row(
    Modifier.fillMaxWidth().border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp)).padding(horizontal = 10.dp, vertical = 6.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text("Ronda $round", Modifier.weight(1f), fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    remainingMs?.let { RoundClock(it) }
  }
}

@Composable
private fun RoundClock(remainingMs: Long) {
  val low = remainingMs < LOW_ROUND_MS
  ClockChip(
    Durations.format(remainingMs), fill = if (low) ClockAlarm else MaterialTheme.colorScheme.primary,
    ink = if (low) Color.White else MaterialTheme.colorScheme.onPrimary,
  )
}

val ClockAlarm = Color(0xFFB3261E)

@Composable
fun ClockChip(text: String, fill: Color, ink: Color) {
  Text(
    text, Modifier.background(fill, RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 4.dp),
    color = ink, fontFamily = FontFamily.Monospace, fontSize = 16.sp, textAlign = TextAlign.Center, maxLines = 1, softWrap = false,
  )
}
