package app.lexico.ui.common

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

enum class VerdictTone(val color: Color) { WIN(Color(0xFF66BB6A)), LOSS(Color(0xFFE5484D)), NEUTRAL(Color.White) }

private val Legible = TextStyle(shadow = Shadow(Color.Black, Offset(0f, 2f), blurRadius = 10f))

@Composable
fun VerdictDialog(
  title: String, tone: VerdictTone, close: () -> Unit, titleSize: TextUnit = 52.sp, celebrate: Boolean = tone == VerdictTone.WIN,
  details: @Composable ColumnScope.() -> Unit,
) {
  Dialog(onDismissRequest = close, properties = DialogProperties(usePlatformDefaultWidth = false)) {
    Box(Modifier.fillMaxSize().pointerInput(Unit) { detectTapGestures { close() } }, contentAlignment = Alignment.Center) {
      if (celebrate) Confetti(Modifier.fillMaxSize())
      Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, color = tone.color, fontSize = titleSize, textAlign = TextAlign.Center, fontWeight = FontWeight.Black, style = Legible)
        details()
      }
    }
  }
}

@Composable
fun VerdictLine(text: String, size: TextUnit = 18.sp, bold: Boolean = false, faded: Boolean = false) {
  val color = if (faded) Color.White.copy(alpha = 0.85f) else Color.White
  Text(text, color = color, fontSize = size, fontWeight = if (bold) FontWeight.Bold else null, textAlign = TextAlign.Center, style = Legible)
}
