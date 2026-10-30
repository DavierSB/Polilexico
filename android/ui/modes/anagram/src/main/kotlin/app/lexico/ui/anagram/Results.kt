package app.lexico.ui.anagram

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import app.lexico.ui.common.Mark
import app.lexico.ui.common.OutcomeBanner
import app.lexico.ui.common.ResultButtons
import app.lexico.ui.common.ResultCard
import app.lexico.ui.common.ResultChip
import app.lexico.ui.common.ResultSection
import app.lexico.ui.common.WordRow

@Composable
internal fun ColumnScope.Revealed(session: AnagramSession, stage: Stage.Revealed) {
  HandBanner(stage.outcome, session.config.limited)
  NextButton(session::next)
  Answers(stage.puzzle, stage.found, "Tenía")
}

@Composable
internal fun ColumnScope.Finished(session: AnagramSession, stage: Stage.Finished, onExit: () -> Unit) {
  val why = if (stage.outcome == Outcome.TIMEOUT) "¡TIEMPO!" else "SIN VIDAS"
  ResultCard(why, "${session.solved}", if (session.solved == 1) "mano resuelta" else "manos resueltas", recordChips(session.record?.best, session.record?.isNew == true))
  ResultButtons("Otra serie", session::restart, "Salir", onExit)
  stage.puzzle?.let { Answers(it, stage.found, "La última mano:") }
}

@Composable
private fun NextButton(onNext: () -> Unit) {
  Button(onClick = onNext, Modifier.fillMaxWidth().height(48.dp)) { Text("Siguiente mano", fontWeight = FontWeight.Bold) }
}

@Composable
private fun HandBanner(outcome: Outcome, limited: Boolean) {
  when (outcome) {
    Outcome.SOLVED -> OutcomeBanner("¡RESUELTA!", "Mano completa", good = true)
    Outcome.GAVE_UP -> OutcomeBanner("TE RENDISTE", if (limited) "Pierdes una vida" else "Mano perdida", good = false)
    Outcome.NO_LIVES -> OutcomeBanner("SIN VIDAS", "Fin de la serie", good = false)
    Outcome.TIMEOUT -> OutcomeBanner("SE ACABÓ EL TIEMPO", "Fin de la serie", good = false)
  }
}

@Composable
private fun ColumnScope.Answers(puzzle: Puzzle, found: List<List<String>>, lead: String) {
  val count = puzzle.answers.size
  ResultSection("$lead ${if (count == 1) "1 anagrama" else "$count anagramas"}", "Letras fijas, subrayadas")
  val accent = MaterialTheme.colorScheme.primary
  Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(4.dp)) {
    puzzle.answers.forEach { WordRow(it.joinToString(""), marked(it, puzzle.fixed, accent), mark = if (it in found) Mark.HIT else Mark.NONE) }
  }
}

private fun marked(tiles: List<String>, fixed: Set<Int>, accent: Color): AnnotatedString = buildAnnotatedString {
  tiles.forEachIndexed { i, tile ->
    if (i in fixed) withStyle(SpanStyle(color = accent, textDecoration = TextDecoration.Underline)) { append(tile) } else append(tile)
  }
}

private fun recordChips(best: Int?, isNew: Boolean): List<ResultChip> = when {
  best == null -> emptyList()
  isNew -> listOf(ResultChip("¡Nuevo récord!", highlighted = true))
  else -> listOf(ResultChip("Récord: ${hands(best)}"))
}
