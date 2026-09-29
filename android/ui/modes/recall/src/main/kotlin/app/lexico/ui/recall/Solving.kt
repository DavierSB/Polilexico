package app.lexico.ui.recall

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.lexico.ui.board.BoardStyle
import app.lexico.ui.board.RackTile
import kotlinx.coroutines.delay

/** Cuanto se ve el ✓ de una palabra armada antes de pasar a la siguiente. */
private const val HIT_PAUSE_MS = 900L

/** Armar la palabra `index` de la ronda. Armada bien, pasa sola a la siguiente. */
@Composable
internal fun Solving(session: RecallSession, index: Int, style: BoardStyle) {
  val word = session.words[index]
  val attempt = remember(session.round, index) { Attempt(word) }
  LaunchedEffect(attempt.hit) {
    if (!attempt.hit) return@LaunchedEffect
    delay(HIT_PAUSE_MS)
    session.answer(hit = true)
  }
  WordHeading(index, session.words.size, word)
  AnswerSlots(attempt, style)
  Feedback(attempt)
  if (!attempt.revealed) Pool(attempt, style)
  AttemptButtons(attempt) { session.answer(hit = false) }
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

@Composable
private fun WordHeading(index: Int, count: Int, word: WordToRecall) {
  Text("Palabra ${index + 1} de $count", style = MaterialTheme.typography.titleMedium)
  Text("${word.tiles.size} fichas · hizo ${word.score} puntos", style = MaterialTheme.typography.bodySmall)
  if (word.fixed.isNotEmpty()) {
    Text("Las fichas sobre casilla ya están en el tablero: tu atril tiene ${word.scrambled.size}.", style = MaterialTheme.typography.bodySmall)
  }
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
    attempt.hit -> Text("¡Esa es!", color = HIT, fontWeight = FontWeight.Bold)
    attempt.revealed -> Text("Era ${attempt.word.text}.", fontWeight = FontWeight.Bold)
    attempt.wrong -> Text("Esa no es. Toca las fichas de arriba para corregirla.", color = MaterialTheme.colorScheme.error)
    else -> Text("Toca las fichas para armarla.", style = MaterialTheme.typography.bodySmall)
  }
}

/** Las fichas barajadas; las ya puestas, casi invisibles. */
@Composable
private fun Pool(attempt: Attempt, style: BoardStyle) {
  val answer = attempt.answer
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
  Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    if (attempt.revealed) return@Row Button(onClick = onNext) { Text("Siguiente") }
    if (attempt.hit) return@Row
    OutlinedButton(onClick = { attempt.answer = attempt.answer.reset() }) { Text("Deshacer") }
    TextButton(onClick = { attempt.revealed = true }) { Text("No la recuerdo") }
  }
}
