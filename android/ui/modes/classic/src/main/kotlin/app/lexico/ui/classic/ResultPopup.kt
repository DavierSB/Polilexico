package app.lexico.ui.classic

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.lexico.ui.common.Confetti
import app.lexico.ui.common.PlayedWordsDialog
import app.lexico.ui.common.SpreadChart
import app.lexico.ui.common.VerdictTone
import kotlinx.coroutines.delay

private const val CONFETTI_LEAD_MS = 1_500L

@Composable
internal fun ResultPopup(view: ClassicView, end: GameEnd, actions: ClassicActions, close: () -> Unit) {
  val celebrate = won(view, end)
  var shown by remember { mutableStateOf(!celebrate) }
  var listing by remember { mutableStateOf(false) }
  LaunchedEffect(Unit) { if (celebrate) delay(CONFETTI_LEAD_MS).also { shown = true } }
  Dialog(onDismissRequest = close, properties = DialogProperties(usePlatformDefaultWidth = false)) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
      if (celebrate) Confetti(Modifier.fillMaxSize())
      if (shown) ResultCard(view, end, actions, { listing = true }, close)
    }
  }
  if (listing) PlayedWordsDialog(playedWords(view)) { listing = false }
}

@Composable
private fun ResultCard(view: ClassicView, end: GameEnd, actions: ClassicActions, onWords: () -> Unit, close: () -> Unit) {
  Surface(Modifier.padding(horizontal = 24.dp).widthIn(max = 420.dp), shape = RoundedCornerShape(28.dp), tonalElevation = 6.dp) {
    Column(
      Modifier.verticalScroll(rememberScrollState()).padding(24.dp), Arrangement.spacedBy(8.dp), Alignment.CenterHorizontally,
    ) {
      ResultLine(verdictText(view, end), verdictSize(view), toneColor(verdictTone(view, end)), FontWeight.Black)
      if (end.byTimeout) ResultLine("por tiempo", 18.sp)
      ResultLine("${view.myScore} – ${view.opponentScore}", 34.sp, weight = FontWeight.Bold)
      ResultLine("${view.me} · ${view.opponent}", 14.sp, MaterialTheme.colorScheme.onSurfaceVariant)
      if (view.moves.isNotEmpty()) SpreadChart(spread(view), Modifier.padding(vertical = 8.dp), height = 150.dp)
      ResultButtons(view, actions, onWords, close)
    }
  }
}

@Composable
private fun ResultLine(text: String, size: TextUnit, color: Color = Color.Unspecified, weight: FontWeight? = null) {
  Text(text, color = color, fontSize = size, fontWeight = weight, textAlign = TextAlign.Center, lineHeight = size * 1.1f)
}

@Composable
private fun ResultButtons(view: ClassicView, actions: ClassicActions, onWords: () -> Unit, close: () -> Unit) {
  val words = playedWords(view).any { it.words.isNotEmpty() }
  Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Button(onClick = { close(); actions.analyze() }, Modifier.fillMaxWidth()) { Text("Analizar partida") }
    if (view.bag == 0) Button(onClick = { close(); actions.analyzeEndgame() }, Modifier.fillMaxWidth()) { Text("Analizar final") }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      if (words) OutlinedButton(onClick = onWords, Modifier.weight(1f)) { Text("Palabras") }
      OutlinedButton(onClick = { close(); actions.export() }, Modifier.weight(1f)) { Text("Exportar") }
    }
    TextButton(onClick = close, Modifier.align(Alignment.End)) { Text("Cerrar") }
  }
}

@Composable
private fun toneColor(tone: VerdictTone): Color = if (tone == VerdictTone.NEUTRAL) MaterialTheme.colorScheme.onSurface else tone.color
