package app.lexico.ui.lists

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.lexico.ui.board.BoardStyle
import app.lexico.ui.board.RackTile
import app.lexico.ui.recall.AnswerBoard
import app.lexico.ui.recall.HIT
import app.lexico.ui.recall.TileLine

@Composable
internal fun ColumnScope.Solving(session: TestSession, stage: TestStage.Solving, style: BoardStyle) {
  val attempt = remember(session.hand) { Attempt(stage.hand.board()) }
  HandBar(session, stage)
  Spacer(Modifier.weight(1f))
  Answer(attempt, style)
  Pool(attempt, style)
  Feedback(attempt, stage.found)
  Spacer(Modifier.weight(1f))
  SolvingButtons(session, attempt)
}

@Stable
private class Attempt(start: AnswerBoard) {
  var answer by mutableStateOf(start)
  var reply: TestReply? by mutableStateOf(null)

  fun change(next: AnswerBoard) {
    answer = next
    reply = null
  }
}

@Composable
private fun HandBar(session: TestSession, stage: TestStage.Solving) {
  Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
    Title("Mano ${session.hand}", Modifier.weight(1f))
    Chip("${stage.found.size} de ${stage.hand.answers.size} " + if (stage.hand.answers.size == 1) "anagrama" else "anagramas")
  }
}

@Composable
private fun Answer(attempt: Attempt, style: BoardStyle) {
  TileLine(attempt.answer.slots.size) { i, size -> Slot(attempt, i, size, style) }
}

@Composable
private fun Slot(attempt: Attempt, slot: Int, size: Dp, style: BoardStyle) {
  val letter = attempt.answer.letters[slot]
  val movable = letter != null && !attempt.answer.isFixed(slot)
  Box(Modifier.clickable(enabled = movable) { attempt.change(attempt.answer.clear(slot)) }) {
    if (letter == null) Hole(style, size) else RackTile(letter, style, size, marked = attempt.answer.isFixed(slot))
  }
}

@Composable
private fun Hole(style: BoardStyle, size: Dp) {
  Box(Modifier.size(size).border(2.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(size * style.rounding)))
}

@Composable
private fun Feedback(attempt: Attempt, found: List<List<String>>) {
  Column(Modifier.fillMaxWidth().heightIn(min = 72.dp), Arrangement.spacedBy(8.dp), Alignment.CenterHorizontally) {
    attempt.reply?.let { ReplyText(it) }
    if (found.isNotEmpty()) Text(found.joinToString("   ") { it.joinToString("") }, style = MaterialTheme.typography.titleMedium,
      fontWeight = FontWeight.Bold, color = HIT, textAlign = TextAlign.Center)
  }
}

@Composable
private fun ReplyText(reply: TestReply) {
  val (text, color) = when (reply) {
    TestReply.LEARNED -> "¡Bien!" to HIT
    TestReply.ALREADY -> "Válida, pero ya está aprendida" to HIT
    TestReply.OUTSIDE -> "Válida, pero no está en esta lista" to HIT
    TestReply.REPEATED -> "Esa ya la encontraste." to MaterialTheme.colorScheme.onSurfaceVariant
    TestReply.INVALID -> "No es válida." to MaterialTheme.colorScheme.error
  }
  Text(text, style = MaterialTheme.typography.titleMedium, color = color, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
}

@Composable
private fun Pool(attempt: Attempt, style: BoardStyle) {
  val answer = attempt.answer
  Box(Modifier.fillMaxWidth().padding(top = 28.dp)) {
    TileLine(answer.pool.size) { i, size ->
      val used = i in answer.used
      Box(Modifier.alpha(if (used) 0.15f else 1f).clickable(enabled = !used) { attempt.change(answer.place(i)) }) { RackTile(answer.pool[i], style, size) }
    }
  }
}

@Composable
private fun SolvingButtons(session: TestSession, attempt: Attempt) {
  Column(Modifier.fillMaxWidth().padding(bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Button(onClick = { send(session, attempt) }, Modifier.fillMaxWidth().height(52.dp), enabled = attempt.answer.isComplete) { Text("Enviar", fontSize = 17.sp) }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      FilledTonalButton(onClick = { attempt.change(attempt.answer.reset()) }, Modifier.weight(1f).height(48.dp)) { Text("Recoger", fontSize = 16.sp) }
      OutlinedButton(onClick = session::pass, Modifier.weight(1f).height(48.dp)) { Text("Ver respuestas", fontSize = 16.sp) }
    }
  }
}

private fun send(session: TestSession, attempt: Attempt) {
  val reply = session.submit(attempt.answer.letters.filterNotNull()) ?: return
  attempt.answer = attempt.answer.reset()
  attempt.reply = reply
}
