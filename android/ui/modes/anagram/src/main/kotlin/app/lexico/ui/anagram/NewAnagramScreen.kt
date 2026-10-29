package app.lexico.ui.anagram

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.lexico.ui.common.ChoiceOptions
import app.lexico.ui.common.Durations
import app.lexico.ui.common.Header
import app.lexico.ui.common.RecordCard
import app.lexico.ui.common.RulesButton
import app.lexico.ui.common.SectionTitle
import app.lexico.ui.common.StartButton
import app.lexico.ui.common.Stepper
import app.lexico.ui.common.TimeField

@Composable
fun NewAnagramScreen(onBack: () -> Unit, recordFor: (AnagramConfig) -> Int, onStart: (AnagramConfig) -> Unit) {
  var total by remember { mutableStateOf(Durations.format(DEFAULT_TOTAL_MS)) }
  var lives by remember { mutableIntStateOf(AnagramConfig.DEFAULT_LIVES) }
  var difficulty by remember { mutableStateOf(Difficulty.MEDIUM) }
  val config = Durations.parse(total)?.takeIf { it > 0 }?.let { AnagramConfig(it, lives, difficulty) }
  var rules by remember { mutableStateOf(false) }
  Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
    Header("Anagrama", onBack)
    RulesButton { rules = true }
    SectionTitle("Dificultad", info = DIFFICULTY_INFO)
    ChoiceOptions(Difficulty.entries.map { it.label }, difficulty.ordinal) { difficulty = Difficulty.entries[it] }
    SectionTitle("Tiempo")
    TimeField(total, { total = it }, "Tiempo de la serie")
    SectionTitle("Vidas", info = "Una palabra no válida o rendirse cuesta una vida. Sin vidas, se acaba la serie.")
    Stepper(lives, AnagramConfig.LIVES) { lives = it }
    config?.let { RecordLine(recordFor(it)) }
    StartButton(enabled = config != null) { config?.let(onStart) }
  }
  if (rules) AnagramRulesDialog { rules = false }
}

@Composable
private fun RecordLine(best: Int) {
  RecordCard(hands(best).takeIf { best > 0 })
}

internal fun hands(n: Int): String = "$n ${if (n == 1) "mano" else "manos"}"

private val DIFFICULTY_INFO =
  "Fácil: siempre hay varios anagramas; basta encontrar uno.\n\n" +
    "Medio: de uno a ${Difficulty.MAX_ANSWERS} anagramas; basta encontrar uno.\n\n" +
    "Difícil: de uno a ${Difficulty.MAX_ANSWERS} anagramas; hay que encontrarlos todos."
