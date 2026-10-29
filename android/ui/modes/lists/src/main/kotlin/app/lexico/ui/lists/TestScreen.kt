package app.lexico.ui.lists

import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.lexico.ui.board.BoardStyle
import app.lexico.ui.common.Header
import app.lexico.ui.common.LocalWordLookup
import app.lexico.ui.recall.HIT

@Composable
fun TestScreen(session: TestSession, style: BoardStyle, onBack: () -> Unit) {
  val lookup = LocalWordLookup.current
  Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
    Header(session.list.name, onBack)
    TestBody(session, style, onBack) { lookup?.invoke(it) }
  }
}

@Composable
private fun ColumnScope.TestBody(session: TestSession, style: BoardStyle, onBack: () -> Unit, onAnswer: (String) -> Unit) {
  TestProgress(session.list)
  Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(14.dp)) { StageContent(session, style, onBack, onAnswer) }
}

@Composable
private fun TestProgress(list: WordList) {
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
      Big("${list.percent}%")
      Text("${list.learned.size} de ${list.members.size} aprendidas", Modifier.padding(bottom = 4.dp), style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    LinearProgressIndicator(progress = { list.percent / 100f }, Modifier.fillMaxWidth())
  }
}

@Composable
private fun ColumnScope.StageContent(session: TestSession, style: BoardStyle, onBack: () -> Unit, onAnswer: (String) -> Unit) {
  when (val stage = session.stage) {
    TestStage.Loading -> Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
    is TestStage.Solving -> Solving(session, stage, style)
    is TestStage.Revealed -> Revealed(session, stage, onAnswer)
    TestStage.Completed -> Completed(session, onBack)
    TestStage.NoHands -> Ending("No quedan manos que se puedan formar con lo que falta de esta lista.", onBack)
  }
}

@Composable
private fun ColumnScope.Revealed(session: TestSession, stage: TestStage.Revealed, onAnswer: (String) -> Unit) {
  Text(revealTitle(stage), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
  Button(onClick = session::next, Modifier.fillMaxWidth()) { Text("Siguiente mano") }
  Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
    stage.hand.answers.forEach { word -> AnswerRow(word, word in stage.found) { onAnswer(WordKeys.wordEntry(WordKeys.of(word))) } }
  }
}

private fun revealTitle(stage: TestStage.Revealed): String {
  val count = stage.hand.answers.size
  val all = if (count == 1) "Tenía un solo anagrama" else "Tenía $count anagramas"
  return "$all; encontraste ${stage.found.size}."
}

@Composable
private fun AnswerRow(word: List<String>, found: Boolean, onClick: () -> Unit) {
  Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable(onClick = onClick).padding(vertical = 4.dp),
    Arrangement.spacedBy(14.dp), Alignment.CenterVertically) {
    Text(if (found) "✓" else "·", color = if (found) HIT else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold, fontSize = 24.sp)
    Text(word.joinToString(""), fontSize = 26.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
  }
}

@Composable
private fun Completed(session: TestSession, onBack: () -> Unit) {
  Text("¡Lista completada!", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = HIT)
  Text("Todas las ${session.list.spec.unit} de la lista están aprendidas.")
  Button(onClick = session::restart, Modifier.fillMaxWidth()) { Text("Empezar de nuevo") }
  OutlinedButton(onClick = onBack, Modifier.fillMaxWidth()) { Text("Salir") }
}

@Composable
private fun Ending(text: String, onBack: () -> Unit) {
  Text(text, style = MaterialTheme.typography.bodyLarge)
  OutlinedButton(onClick = onBack, Modifier.fillMaxWidth()) { Text("Salir") }
}
