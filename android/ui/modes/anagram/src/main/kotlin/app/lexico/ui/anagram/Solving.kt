package app.lexico.ui.anagram

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import app.lexico.ui.common.Compact
import app.lexico.ui.common.Mulish
import app.lexico.ui.recall.AnswerBoard
import app.lexico.ui.recall.EmptySlot
import app.lexico.ui.recall.HIT
import app.lexico.ui.recall.TileLine

@Composable
internal fun ColumnScope.Solving(session: AnagramSession, stage: Stage.Solving, style: BoardStyle) {
  val attempt = remember(session.hand) { Attempt(stage.puzzle.board()) }
  Spacer(Modifier.weight(1f))
  HandCard(session, stage, attempt, style)
  Pool(attempt, style)
  if (session.config.difficulty.findAll) FoundWords(stage.found)
  Spacer(Modifier.weight(1f))
  SolvingButtons(session, attempt)
}

@Stable
private class Attempt(start: AnswerBoard) {
  var answer by mutableStateOf(start)
  var reply: Reply? by mutableStateOf(null)

  fun change(next: AnswerBoard) {
    answer = next
    reply = null
  }
}

@Composable
private fun HandCard(session: AnagramSession, stage: Stage.Solving, attempt: Attempt, style: BoardStyle) {
  Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
    Column(Modifier.padding(vertical = 20.dp, horizontal = 12.dp), Arrangement.spacedBy(14.dp), Alignment.CenterHorizontally) {
      Text(handTitle(session, stage), fontFamily = Mulish, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, letterSpacing = 2.sp,
        color = MaterialTheme.colorScheme.primary)
      TileLine(attempt.answer.slots.size) { i, size -> Slot(attempt, i, size, style) }
      attempt.reply?.let { ReplyText(it, session.config.limited) }
    }
  }
}

private fun handTitle(session: AnagramSession, stage: Stage.Solving): String {
  val title = "MANO ${session.hand}"
  if (!session.config.difficulty.findAll) return title
  return "$title · ${stage.found.size} DE ${stage.puzzle.answers.size}"
}

@Composable
private fun Slot(attempt: Attempt, slot: Int, size: Dp, style: BoardStyle) {
  val letter = attempt.answer.letters[slot]
  val movable = letter != null && !attempt.answer.isFixed(slot)
  Box(Modifier.clickable(enabled = movable) { attempt.change(attempt.answer.clear(slot)) }) {
    if (letter == null) EmptySlot(style, size) else RackTile(letter, style, size, marked = attempt.answer.isFixed(slot))
  }
}

@Composable
private fun ReplyText(reply: Reply, limited: Boolean) {
  val (text, color) = when (reply) {
    Reply.INVALID -> (if (limited) "No es válida: pierdes una vida." else "No es válida.") to MaterialTheme.colorScheme.error
    Reply.REPEATED -> "Esa ya la encontraste." to MaterialTheme.colorScheme.onSurfaceVariant
    Reply.FOUND -> "✓  ¡Esa es! Busca otra." to HIT
    Reply.SOLVED -> "✓  ¡Esa es!" to HIT
  }
  Text(text, style = MaterialTheme.typography.bodyMedium, color = color, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
}

@Composable
private fun Pool(attempt: Attempt, style: BoardStyle) {
  val answer = attempt.answer
  Box(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f), RoundedCornerShape(14.dp)).padding(10.dp)) {
    TileLine(answer.pool.size) { i, size ->
      val used = i in answer.used
      Box(Modifier.alpha(if (used) 0.15f else 1f).clickable(enabled = !used) { attempt.change(answer.place(i)) }) {
        RackTile(answer.pool[i], style, size)
      }
    }
  }
}

@Composable
private fun FoundWords(found: List<List<String>>) {
  if (found.isEmpty()) return
  Text("Encontradas: " + found.joinToString("  ") { it.joinToString("") }, Modifier.fillMaxWidth(), fontWeight = FontWeight.Bold,
    textAlign = TextAlign.Center)
}

@Composable
private fun SolvingButtons(session: AnagramSession, attempt: Attempt) {
  Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), Arrangement.spacedBy(6.dp)) {
    Button(onClick = { send(session, attempt) }, Modifier.weight(1f), contentPadding = Compact, enabled = attempt.answer.isComplete) {
      Text("Enviar")
    }
    FilledTonalButton(onClick = { attempt.change(attempt.answer.reset()) }, Modifier.weight(1f), contentPadding = Compact) { Text("Recoger") }
    OutlinedButton(onClick = session::giveUp, Modifier.weight(1f), contentPadding = Compact) { Text("Rendirse") }
  }
}

private fun send(session: AnagramSession, attempt: Attempt) {
  val reply = session.submit(attempt.answer.letters.filterNotNull()) ?: return
  attempt.answer = attempt.answer.reset()
  attempt.reply = reply
}
