package app.lexico.ui.classic

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.lexico.ui.common.Confetti

private val WinGreen = Color(0xFF66BB6A)
private val LossRed = Color(0xFFE5484D)
private val Legible = TextStyle(shadow = Shadow(Color.Black, Offset(0f, 2f), blurRadius = 10f))

@Composable
internal fun EndingDialog(view: ClassicView, ending: Ending, close: () -> Unit) {
  AlertDialog(
    onDismissRequest = close,
    title = { Text(endingTitle(view, ending.reason)) },
    text = { EndingSummary(view, ending) },
    confirmButton = { TextButton(onClick = close) { Text("Continuar") } },
  )
}

@Composable
internal fun ResultDialog(view: ClassicView, end: GameEnd, close: () -> Unit) {
  Dialog(onDismissRequest = close, properties = DialogProperties(usePlatformDefaultWidth = false)) {
    Box(Modifier.fillMaxSize().pointerInput(Unit) { detectTapGestures { close() } }, contentAlignment = Alignment.Center) {
      if (won(view, end)) Confetti(Modifier.fillMaxSize())
      Verdict(view, end)
    }
  }
}

private fun endingTitle(view: ClassicView, reason: EndingReason): String = when (reason) {
  EndingReason.WENT_OUT -> wentOutTitle(view)
  EndingReason.PASSES -> "Juego terminado por 4 pases consecutivos"
  EndingReason.NEUTRAL_TURNS -> "Juego terminado por 12 turnos neutros consecutivos"
}

private fun wentOutTitle(view: ClassicView): String = when {
  view.rack.isNotEmpty() -> "${view.opponent} se pegó"
  view.local -> "${view.me} se pegó"
  else -> "Te pegaste"
}

@Composable
private fun EndingSummary(view: ClassicView, ending: Ending) {
  Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
    Text("Descuento por fichas", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    DeltaLine(view.me, ending.myDelta)
    DeltaLine(view.opponent, ending.opponentDelta)
  }
}

@Composable
private fun DeltaLine(name: String, delta: Int) {
  Row(Modifier.fillMaxWidth()) {
    Text(name, Modifier.weight(1f), fontSize = 18.sp)
    Text(signed(delta), fontSize = 18.sp, fontWeight = FontWeight.Bold)
  }
}

@Composable
private fun Verdict(view: ClassicView, end: GameEnd) {
  Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Text(verdictText(view, end), color = verdictColor(view, end), fontSize = verdictSize(view), textAlign = TextAlign.Center, fontWeight = FontWeight.Black, style = Legible)
    if (end.byTimeout) Text("por tiempo", color = Color.White, fontSize = 18.sp, style = Legible)
    Text("${view.myScore} – ${view.opponentScore}", color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Bold, style = Legible)
    Text("${view.me} · ${view.opponent}", color = Color.White.copy(alpha = 0.85f), fontSize = 14.sp, style = Legible)
  }
}

private fun won(view: ClassicView, end: GameEnd): Boolean =
  if (view.local) end.winner != null else end.winner == Side.ME && !end.byTimeout

private fun verdictSize(view: ClassicView) = if (view.local) 40.sp else 52.sp

private fun verdictText(view: ClassicView, end: GameEnd): String = when {
  view.local -> "¡${localVerdict(view, end)}!".takeIf { end.winner != null } ?: "Empate"
  won(view, end) -> "¡Ganaste!"
  end.byTimeout || end.winner == Side.OPPONENT -> "Perdiste"
  else -> "Empate"
}

private fun verdictColor(view: ClassicView, end: GameEnd): Color = when {
  won(view, end) -> WinGreen
  end.byTimeout || end.winner == Side.OPPONENT -> LossRed
  else -> Color.White
}
