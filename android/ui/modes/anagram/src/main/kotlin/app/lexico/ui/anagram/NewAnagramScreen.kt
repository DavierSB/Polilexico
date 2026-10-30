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
import app.lexico.ui.common.CreateRoomButton
import app.lexico.ui.common.Durations
import app.lexico.ui.common.Header
import app.lexico.ui.common.RACE_INFO
import app.lexico.ui.common.RACE_LIVES
import app.lexico.ui.common.RecordCard
import app.lexico.ui.common.RulesButton
import app.lexico.ui.common.SectionTitle
import app.lexico.ui.common.SoloOrWifiTabs
import app.lexico.ui.common.StartButton
import app.lexico.ui.common.Stepper
import app.lexico.ui.common.TimeField
import app.lexico.ui.common.WifiForm

data class AnagramRace(val name: String, val config: AnagramConfig, val goal: Int)

@Composable
fun NewAnagramScreen(
  onBack: () -> Unit, recordFor: (AnagramConfig) -> Int, onStart: (AnagramConfig) -> Unit,
  onHost: (AnagramRace) -> Unit, onJoin: (name: String) -> Unit,
) {
  var wifi by remember { mutableStateOf(false) }
  var rules by remember { mutableStateOf(false) }
  Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
    Header("Anagrama", onBack)
    SoloOrWifiTabs(wifi) { wifi = it }
    RulesButton { rules = true }
    if (wifi) WifiForm(onJoin) { name -> RaceSettings(name, onHost) } else SoloSettings(recordFor, onStart)
  }
  if (rules) AnagramRulesDialog { rules = false }
}

@Composable
private fun SoloSettings(recordFor: (AnagramConfig) -> Int, onStart: (AnagramConfig) -> Unit) {
  var total by remember { mutableStateOf(Durations.format(DEFAULT_TOTAL_MS)) }
  var lives by remember { mutableIntStateOf(AnagramConfig.DEFAULT_LIVES) }
  var difficulty by remember { mutableStateOf(Difficulty.MEDIUM) }
  val config = Durations.parse(total)?.takeIf { it > 0 }?.let { AnagramConfig(it, lives, difficulty) }
  DifficultySection(difficulty) { difficulty = it }
  SectionTitle("Tiempo")
  TimeField(total, { total = it }, "Tiempo de la serie")
  SectionTitle("Vidas", info = "Una palabra no válida o rendirse cuesta una vida. Sin vidas, se acaba la serie.")
  Stepper(lives, AnagramConfig.LIVES) { lives = it }
  config?.let { RecordLine(recordFor(it)) }
  StartButton(enabled = config != null) { config?.let(onStart) }
}

@Composable
private fun RaceSettings(name: String, onHost: (AnagramRace) -> Unit) {
  var total by remember { mutableStateOf(Durations.format(DEFAULT_TOTAL_MS)) }
  var goal by remember { mutableIntStateOf(DEFAULT_GOAL) }
  var difficulty by remember { mutableStateOf(Difficulty.MEDIUM) }
  val totalMs = Durations.parse(total)?.takeIf { it > 0 }
  DifficultySection(difficulty) { difficulty = it }
  SectionTitle("Meta", info = RACE_INFO)
  Stepper(goal, GOALS) { goal = it }
  SectionTitle("Tiempo límite")
  TimeField(total, { total = it }, "Tiempo")
  CreateRoomButton(enabled = name.isNotEmpty() && totalMs != null) {
    totalMs?.let { onHost(AnagramRace(name, AnagramConfig(it, RACE_LIVES, difficulty), goal)) }
  }
}

@Composable
private fun DifficultySection(difficulty: Difficulty, onChange: (Difficulty) -> Unit) {
  SectionTitle("Dificultad", info = DIFFICULTY_INFO)
  ChoiceOptions(Difficulty.entries.map { it.label }, difficulty.ordinal) { onChange(Difficulty.entries[it]) }
}

@Composable
private fun RecordLine(best: Int) {
  RecordCard(hands(best).takeIf { best > 0 })
}

private const val DEFAULT_GOAL = 10

private val GOALS = 1..50

internal fun hands(n: Int): String = "$n ${if (n == 1) "mano" else "manos"}"

private val DIFFICULTY_INFO =
  "Fácil: siempre hay varios anagramas; basta encontrar uno.\n\n" +
    "Medio: de uno a ${Difficulty.MAX_ANSWERS} anagramas; basta encontrar uno.\n\n" +
    "Difícil: de uno a ${Difficulty.MAX_ANSWERS} anagramas; hay que encontrarlos todos."
