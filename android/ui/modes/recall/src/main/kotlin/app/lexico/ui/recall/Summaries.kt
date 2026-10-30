package app.lexico.ui.recall

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.lexico.ui.common.Mark
import app.lexico.ui.common.ResultButtons
import app.lexico.ui.common.ResultCard
import app.lexico.ui.common.ResultChip
import app.lexico.ui.common.ResultSection
import app.lexico.ui.common.WordRow

@Composable
internal fun ColumnScope.RoundDone(session: RecallSession, onNext: () -> Unit, onExit: () -> Unit) {
  val answers = session.answers
  Score("RECORDASTE", answers.count { it.hit }, session.words.size, pointsChip(answers), progressChip(session))
  ResultButtons("Siguiente partida", onNext, "Salir", onExit)
  AnswerList(answers, unanswered(session))
}

@Composable
internal fun ColumnScope.SeriesDone(session: RecallSession, onExit: () -> Unit) {
  Score("EN LA SERIE", session.recalled, session.asked, *listOfNotNull(session.record?.let(::recordChip)).toTypedArray())
  ResultButtons("Otra serie", session::restart, "Menú", onExit)
  AnswerList(session.answers)
}

@Composable
private fun Score(title: String, hits: Int, total: Int, vararg chips: ResultChip) {
  ResultCard(title, "$hits", "/ $total", chips.toList(), progress = if (total > 0) hits.toFloat() / total else 0f)
}

@Composable
private fun ColumnScope.AnswerList(answers: List<Answer>, unanswered: List<WordToRecall> = emptyList()) {
  ResultSection("Las palabras de la partida", "Toca para su ficha")
  Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(4.dp)) {
    answers.sortedByDescending { it.word.score }.forEach { WordRow(it.word.text, score = it.word.score, mark = if (it.hit) Mark.HIT else Mark.MISS) }
    unanswered.sortedByDescending { it.score }.forEach { WordRow(it.text, score = it.score, mark = Mark.NONE) }
  }
}

private fun unanswered(session: RecallSession): List<WordToRecall> = session.words.drop(session.answers.size)

private fun pointsChip(answers: List<Answer>): ResultChip =
  ResultChip("${answers.filter { it.hit }.sumOf { it.word.score }} de ${answers.sumOf { it.word.score }} pts", highlighted = answers.any { it.hit })

private fun progressChip(session: RecallSession): ResultChip =
  when {
    session.config.rounds > 0 -> ResultChip("Ronda ${session.round} de ${session.config.rounds}")
    session.config.shared -> ResultChip("Ronda ${session.round}")
    else -> livesChip(session.lives)
  }

private fun livesChip(lives: Int): ResultChip = ResultChip(if (lives == 1) "1 vida" else "$lives vidas")

private fun recordChip(record: Record): ResultChip =
  if (record.isNew) ResultChip("¡Nuevo récord!", highlighted = true) else ResultChip("Récord: ${words(record.best)}")
