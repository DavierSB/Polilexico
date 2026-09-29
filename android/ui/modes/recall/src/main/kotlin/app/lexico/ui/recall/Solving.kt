package app.lexico.ui.recall

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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

/** Cuanto se ve el ✓ de una palabra armada antes de pasar a la siguiente. */
private const val HIT_PAUSE_MS = 900L

/**
 * Armar la palabra `index` de la ronda: arriba, como va la ronda; en medio, la tarjeta con la
 * palabra y el atril debajo. Armada bien, pasa sola a la siguiente.
 */
@Composable
internal fun ColumnScope.Solving(session: RecallSession, index: Int, style: BoardStyle) {
  val word = session.words[index]
  val attempt = remember(session.round, index) { Attempt(word) }
  LaunchedEffect(attempt.hit) {
    if (!attempt.hit) return@LaunchedEffect
    delay(HIT_PAUSE_MS)
    session.answer(hit = true)
  }
  RoundProgress(session.answers, session.words.size)
  Spacer(Modifier.weight(1f))
  WordCard(index, session.words.size, attempt, style)
  if (!attempt.revealed) Pool(attempt, style)
  Spacer(Modifier.weight(1f))
  AttemptButtons(attempt) { session.answer(hit = false) }
}

/** Un punto por palabra de la ronda: verde si la armaste, rojo si no, resaltado el de la que va. */
@Composable
private fun RoundProgress(answers: List<Answer>, count: Int) {
  Row(Modifier.fillMaxWidth().padding(top = 4.dp), Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally), Alignment.CenterVertically) {
    repeat(count) { i -> ProgressDot(answers.getOrNull(i)?.hit, current = i == answers.size) }
  }
}

@Composable
private fun ProgressDot(hit: Boolean?, current: Boolean) {
  val color = when (hit) {
    true -> HIT
    false -> MaterialTheme.colorScheme.error
    null -> if (current) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
  }
  val size = if (current) 14.dp else 10.dp
  val dot = Modifier.size(size).background(color, CircleShape)
  Box(if (current) dot.border(3.dp, MaterialTheme.colorScheme.primaryContainer, CircleShape) else dot)
}

/** La tarjeta de la palabra: cual es, sus puntos, la palabra que se va armando y como va. */
@Composable
private fun WordCard(index: Int, count: Int, attempt: Attempt, style: BoardStyle) {
  Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
    Column(Modifier.padding(vertical = 20.dp, horizontal = 12.dp), Arrangement.spacedBy(14.dp), Alignment.CenterHorizontally) {
      WordHeading(index, count, attempt.word)
      AnswerSlots(attempt, style)
      Feedback(attempt)
    }
  }
}

/** Un intento de armar una palabra: las fichas puestas y si se pidio verla. */
@Stable
private class Attempt(val word: WordToRecall) {
  var answer by mutableStateOf(AnswerBoard.start(word))
  var revealed by mutableStateOf(false)

  val hit: Boolean get() = answer.isComplete && answer.letters == word.tiles
  val wrong: Boolean get() = answer.isComplete && !hit

  /** Lo que se ve en la palabra: lo armado o, si se revelo, la palabra. */
  val slots: List<String?> get() = if (revealed) word.tiles else answer.letters
}

/** "PALABRA 3 DE 10", sus puntos en grande y cuantas fichas tiene. */
@Composable
private fun WordHeading(index: Int, count: Int, word: WordToRecall) {
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Text("PALABRA ${index + 1} DE $count", fontFamily = Mulish, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp,
      letterSpacing = 2.sp, color = MaterialTheme.colorScheme.primary)
    Row(verticalAlignment = Alignment.Bottom) {
      Text("${word.score}", fontFamily = Mulish, fontWeight = FontWeight.Black, fontSize = 48.sp, lineHeight = 48.sp)
      Text(" puntos", Modifier.padding(bottom = 7.dp), fontFamily = Mulish, fontWeight = FontWeight.Bold, fontSize = 17.sp)
    }
    Text("${word.tiles.size} fichas", fontFamily = Mulish, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
    if (word.fixed.isNotEmpty()) Hint("Las fichas con borde ya están en el tablero: tu atril tiene ${word.scrambled.size}.")
  }
}

@Composable
private fun Hint(text: String, color: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
  Text(text, Modifier.padding(top = 4.dp), style = MaterialTheme.typography.bodySmall, color = color, textAlign = TextAlign.Center)
}

/** La palabra que se va armando: tocar una ficha la devuelve abajo. */
@Composable
private fun AnswerSlots(attempt: Attempt, style: BoardStyle) {
  val slots = attempt.slots
  TileLine(slots.size) { i, size -> AnswerSlot(attempt, i, slots[i], style, size) }
}

/** Una casilla de la palabra: vacia, con una ficha fija del tablero o con una del atril. */
@Composable
private fun AnswerSlot(attempt: Attempt, slot: Int, letter: String?, style: BoardStyle, size: Dp) {
  val fixed = attempt.answer.isFixed(slot)
  val movable = !attempt.revealed && !attempt.hit && letter != null && !fixed
  Box(Modifier.clickable(enabled = movable) { attempt.answer = attempt.answer.clear(slot) }) {
    when {
      letter == null -> EmptySlot(style, size)
      fixed -> FixedTile(letter, style, size)
      else -> RackTile(letter, style, size)
    }
  }
}

@Composable
private fun Feedback(attempt: Attempt) {
  when {
    attempt.hit -> Verdict("✓  ¡Esa es!", HIT)
    attempt.revealed -> Verdict("Era ${attempt.word.text}", MaterialTheme.colorScheme.onSurface)
    attempt.wrong -> Hint("Esa no es. Toca las fichas de la palabra para corregirla.", MaterialTheme.colorScheme.error)
    else -> Hint("Toca las fichas del atril para armarla.")
  }
}

@Composable
private fun Verdict(text: String, color: Color) {
  Text(text, fontFamily = Mulish, fontWeight = FontWeight.Black, fontSize = 20.sp, color = color)
}

/** Las fichas barajadas, sobre el atril; las ya puestas, casi invisibles. */
@Composable
private fun Pool(attempt: Attempt, style: BoardStyle) {
  val answer = attempt.answer
  Box(Modifier.fillMaxWidth().padding(top = 4.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f), RoundedCornerShape(14.dp)).padding(10.dp)) { PoolTiles(attempt, answer, style) }
}

@Composable
private fun PoolTiles(attempt: Attempt, answer: AnswerBoard, style: BoardStyle) {
  TileLine(answer.pool.size) { i, size ->
    val used = i in answer.used
    Box(Modifier.alpha(if (used) 0.15f else 1f).clickable(enabled = !used && !attempt.hit) { attempt.answer = answer.place(i) }) {
      RackTile(answer.pool[i], style, size)
    }
  }
}

/** Revelada: pasar a la siguiente. Si no: deshacer o rendirse. */
@Composable
private fun AttemptButtons(attempt: Attempt, onNext: () -> Unit) {
  Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), Arrangement.spacedBy(8.dp)) {
    if (attempt.revealed) return@Row Button(onClick = onNext, Modifier.weight(1f)) { Text("Siguiente") }
    if (attempt.hit) return@Row
    OutlinedButton(onClick = { attempt.answer = attempt.answer.reset() }, Modifier.weight(1f)) { Text("Deshacer") }
    TextButton(onClick = { attempt.revealed = true }, Modifier.weight(1f)) { Text("No la recuerdo") }
  }
}
