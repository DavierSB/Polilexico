package app.lexico.ui.common

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp

/** Por debajo de esto el reloj se pone rojo. */
private const val LOW_TIME_MS = 30_000L

private val LowTimePink = Color(0xFFF2B8B5)

/** El tiempo que queda del turno, grande y centrado; rojo en los ultimos 30 segundos. */
@Composable
fun TurnClock(remainingMs: Long) {
  Text(
    Durations.format(remainingMs), Modifier.fillMaxWidth(), fontSize = 22.sp, fontWeight = FontWeight.Bold,
    fontFamily = FontFamily.Monospace, textAlign = TextAlign.Center,
    color = if (remainingMs < LOW_TIME_MS) LowTimePink else MaterialTheme.colorScheme.onSurface,
  )
}
