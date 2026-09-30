package app.lexico.ui.recall

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.lexico.ui.board.BoardStyle
import app.lexico.ui.board.RackTile
import app.lexico.ui.common.Mulish
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Cuanto se ve el ✓ de una palabra armada antes de pasar a la siguiente. */
private const val HIT_PAUSE_MS = 900L

/**
 * Armar la palabra `stage.index` de la partida: la tarjeta con la palabra, el atril debajo y los
 * botones. Al enviarla, su veredicto; si era esa, pasa sola a la siguiente.
 */
@Composable
internal fun ColumnScope.Solving(session: RecallSession, stage: Stage.Solving, style: BoardStyle) {
  val word = session.words[stage.index]
  val attempt = remember(session.round, stage.index) { Attempt(word) }
  LaunchedEffect(stage.verdict) {
    if (stage.verdict != Verdict.HIT) return@LaunchedEffect
    delay(HIT_PAUSE_MS)
    session.next()
  }
  Spacer(Modifier.weight(1f))
  WordCard(stage, session.words.size, attempt, style)
  if (stage.verdict == null) Pool(attempt, style)
  Spacer(Modifier.weight(1f))
  AttemptButtons(session, stage.verdict, attempt)
}

/** Un intento de armar una palabra: las fichas puestas, si se esta consultando y si se rechazo por no valida. */
@Stable
private class Attempt(val word: WordToRecall) {
  var answer by mutableStateOf(AnswerBoard.start(word))
  var checking by mutableStateOf(false)
  var rejected by mutableStateOf(false)

  fun change(next: AnswerBoard) {
    answer = next
    rejected = false
  }

  /** Lo que se ve en la palabra: lo armado o, ya resuelta, la palabra. */
  fun slots(verdict: Verdict?): List<String?> = if (verdict == null) answer.letters else word.tiles
}

/** La tarjeta de la palabra: cual es, la palabra que se va armando y, al enviarla, el veredicto. */
@Composable
private fun WordCard(stage: Stage.Solving, count: Int, attempt: Attempt, style: BoardStyle) {
  Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
    Column(Modifier.padding(vertical = 20.dp, horizontal = 12.dp), Arrangement.spacedBy(14.dp), Alignment.CenterHorizontally) {
      Text("PALABRA ${stage.index + 1} DE $count", fontFamily = Mulish, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp,
        letterSpacing = 2.sp, color = MaterialTheme.colorScheme.primary)
      AnswerSlots(attempt, stage.verdict, style)
      Feedback(attempt, stage.verdict)
    }
  }
}

/** La palabra que se va armando: tocar una ficha del atril la devuelve abajo. */
@Composable
private fun AnswerSlots(attempt: Attempt, verdict: Verdict?, style: BoardStyle) {
  val slots = attempt.slots(verdict)
  TileLine(slots.size) { i, size -> AnswerSlot(attempt, i, slots[i], verdict == null, style, size) }
}

/** Una casilla de la palabra: vacia o con una ficha (las fijas del tablero no se mueven). */
@Composable
private fun AnswerSlot(attempt: Attempt, slot: Int, letter: String?, open: Boolean, style: BoardStyle, size: Dp) {
  val movable = open && !attempt.checking && letter != null && !attempt.answer.isFixed(slot)
  Box(Modifier.clickable(enabled = movable) { attempt.change(attempt.answer.clear(slot)) }) {
    if (letter == null) EmptySlot(style, size) else RackTile(letter, style, size)
  }
}

/** El veredicto; en void, el aviso de que lo armado no es valido. */
@Composable
private fun Feedback(attempt: Attempt, verdict: Verdict?) {
  when (verdict) {
    Verdict.HIT -> VerdictText("✓  ¡Esa es!", HIT)
    Verdict.INVALID -> Missed("No es válida", attempt.word)
    Verdict.NOT_PLAYED -> Missed("La palabra que se jugó no fue esa", attempt.word)
    Verdict.FORGOTTEN -> Missed(null, attempt.word)
    null -> if (attempt.rejected) Hint("No es válida. Corrígela tocando sus fichas.", MaterialTheme.colorScheme.error)
  }
}

/** Por que no era (si hace falta) y cual era. */
@Composable
private fun Missed(why: String?, word: WordToRecall) {
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    why?.let { VerdictText(it, MaterialTheme.colorScheme.error) }
    Text("Era ${word.text}", fontFamily = Mulish, fontWeight = FontWeight.Bold, fontSize = 17.sp)
  }
}

@Composable
private fun VerdictText(text: String, color: Color) {
  Text(text, fontFamily = Mulish, fontWeight = FontWeight.Black, fontSize = 20.sp, color = color, textAlign = TextAlign.Center)
}

@Composable
private fun Hint(text: String, color: Color) {
  Text(text, style = MaterialTheme.typography.bodySmall, color = color, textAlign = TextAlign.Center)
}

/** Las fichas barajadas, sobre el atril; las ya puestas, casi invisibles. */
@Composable
private fun Pool(attempt: Attempt, style: BoardStyle) {
  val answer = attempt.answer
  Box(Modifier.fillMaxWidth().padding(top = 4.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f), RoundedCornerShape(14.dp)).padding(10.dp)) {
    TileLine(answer.pool.size) { i, size ->
      val used = i in answer.used
      Box(Modifier.alpha(if (used) 0.15f else 1f).clickable(enabled = !used && !attempt.checking) { attempt.change(answer.place(i)) }) {
        RackTile(answer.pool[i], style, size)
      }
    }
  }
}

/** Sin veredicto: enviar o rendirse. Fallada: pasar a la siguiente. Acertada: nada, pasa sola. */
@Composable
private fun AttemptButtons(session: RecallSession, verdict: Verdict?, attempt: Attempt) {
  val scope = rememberCoroutineScope()
  Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), Arrangement.spacedBy(8.dp)) {
    when (verdict) {
      Verdict.HIT -> Unit
      null -> {
        val ready = attempt.answer.isComplete && !attempt.checking && !attempt.rejected
        Button(onClick = { scope.launch { send(session, attempt) } }, Modifier.weight(1f), enabled = ready) { Text("Enviar") }
        TextButton(onClick = session::forget, Modifier.weight(1f), enabled = !attempt.checking) { Text("No la recuerdo") }
      }
      else -> Button(onClick = session::next, Modifier.weight(1f)) { Text("Siguiente") }
    }
  }
}

/** Envia lo armado; si se rechaza (void), se marca para corregirlo. */
private suspend fun send(session: RecallSession, attempt: Attempt) {
  attempt.checking = true
  val result = session.submit(attempt.answer.letters.filterNotNull())
  attempt.checking = false
  if (result == Submission.Rejected) attempt.rejected = true
}
