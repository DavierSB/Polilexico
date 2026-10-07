package app.lexico.soundboard

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.lexico.ui.common.Confetti

private val WinGreen = Color(0xFF66BB6A)
private val Legible = TextStyle(shadow = Shadow(Color.Black, Offset(0f, 2f), blurRadius = 10f))

@Composable
fun CelebrationOverlay(res: Int, close: () -> Unit) {
  val ctx = LocalContext.current
  LaunchedEffect(res) { celebrate(ctx, res) }
  Dialog(onDismissRequest = close, properties = DialogProperties(usePlatformDefaultWidth = false)) {
    Box(Modifier.fillMaxSize().pointerInput(Unit) { detectTapGestures { close() } }, contentAlignment = Alignment.Center) {
      Confetti(Modifier.fillMaxSize())
      Verdict()
    }
  }
}

@Composable
private fun Verdict() {
  Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Text("¡Ganaste!", color = WinGreen, fontSize = 52.sp, fontWeight = FontWeight.Black, style = Legible)
    Text("412 – 380", color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Bold, style = Legible)
    Text("Tú · Pelusa", color = Color.White.copy(alpha = 0.85f), fontSize = 14.sp, style = Legible)
  }
}
