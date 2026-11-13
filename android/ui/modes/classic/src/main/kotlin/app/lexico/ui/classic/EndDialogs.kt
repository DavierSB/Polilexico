package app.lexico.ui.classic

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.lexico.ui.common.VerdictTone

@Composable
internal fun EndingDialog(view: ClassicView, ending: Ending, close: () -> Unit) {
  AlertDialog(
    onDismissRequest = close,
    title = { Text(endingTitle(view, ending.reason)) },
    text = { EndingSummary(view, ending) },
    confirmButton = { TextButton(onClick = close) { Text("Continuar") } },
  )
}

private fun endingTitle(view: ClassicView, reason: EndingReason): String = when (reason) {
  EndingReason.WENT_OUT -> wentOutTitle(view)
  EndingReason.PASSES -> "Juego terminado por 4 pases consecutivos"
  EndingReason.NEUTRAL_TURNS -> "Juego terminado por 12 turnos neutros consecutivos"
}

private fun wentOutTitle(view: ClassicView): String = when {
  view.rack.isNotEmpty() -> "${view.opponent} se pegó"
  view.local || view.watching -> "${view.me} se pegó"
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

internal fun won(view: ClassicView, end: GameEnd): Boolean =
  if (view.local || view.watching) end.winner != null else end.winner == Side.ME && !end.byTimeout

internal fun verdictSize(view: ClassicView) = if (view.local || view.watching) 40.sp else 52.sp

internal fun verdictText(view: ClassicView, end: GameEnd): String = when {
  view.watching -> winnerName(view, end)?.let { "$it ganó" } ?: "Empate"
  view.local -> "¡${localVerdict(view, end)}!".takeIf { end.winner != null } ?: "Empate"
  won(view, end) -> "¡Ganaste!"
  end.byTimeout || end.winner == Side.OPPONENT -> "Perdiste"
  else -> "Empate"
}

internal fun winnerName(view: ClassicView, end: GameEnd): String? = when (end.winner) {
  Side.ME -> view.me
  Side.OPPONENT -> view.opponent
  null -> null
}

internal fun verdictTone(view: ClassicView, end: GameEnd): VerdictTone = when {
  won(view, end) -> VerdictTone.WIN
  end.byTimeout || end.winner == Side.OPPONENT -> VerdictTone.LOSS
  else -> VerdictTone.NEUTRAL
}
