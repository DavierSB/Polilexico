package app.lexico.ui.sprint

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
import app.lexico.ui.common.ChallengeModeSelector
import app.lexico.ui.common.ChoiceOptions
import app.lexico.ui.common.Durations
import app.lexico.ui.common.Header
import app.lexico.ui.common.RecordCard
import app.lexico.ui.common.RulesButton
import app.lexico.ui.common.SectionTitle
import app.lexico.ui.common.StartButton
import app.lexico.ui.common.Stepper
import app.lexico.ui.common.TimeField

const val DEFAULT_TOTAL_MS = 300_000L

enum class Difficulty(val engineName: String, val label: String) {
  EASY("easy", "Fácil"), NORMAL("normal", "Normal"), HARD("hard", "Difícil"),
}

data class SprintConfig(
  val totalMs: Long = DEFAULT_TOTAL_MS,
  val lives: Int = DEFAULT_LIVES,
  val single: Boolean = true,
  val difficulty: Difficulty = Difficulty.NORMAL,
) {
  companion object {
    const val DEFAULT_LIVES = 3
    val LIVES = 1..5
  }
}

@Composable
fun NewSprintScreen(onBack: () -> Unit, recordFor: (SprintConfig) -> Int, onStart: (SprintConfig) -> Unit) {
  var total by remember { mutableStateOf(Durations.format(DEFAULT_TOTAL_MS)) }
  var lives by remember { mutableIntStateOf(SprintConfig.DEFAULT_LIVES) }
  var single by remember { mutableStateOf(true) }
  var difficulty by remember { mutableStateOf(Difficulty.NORMAL) }
  val config = Durations.parse(total)?.takeIf { it > 0 }?.let { SprintConfig(it, lives, single, difficulty) }
  var rules by remember { mutableStateOf(false) }
  Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
    Header("Scrabble Sprint", onBack)
    RulesButton { rules = true }
    DifficultySection(difficulty) { difficulty = it }
    SectionTitle("Tiempo")
    TimeField(total, { total = it }, "Tiempo de la serie")
    SectionTitle("Vidas")
    Stepper(lives, SprintConfig.LIVES) { lives = it }
    ChallengeModeSelector(single, { single = it }, singleHint = SINGLE_HINT)
    config?.let { RecordLine(recordFor(it)) }
    StartButton(enabled = config != null) { config?.let(onStart) }
  }
  if (rules) SprintRulesDialog { rules = false }
}

@Composable
private fun DifficultySection(difficulty: Difficulty, onChange: (Difficulty) -> Unit) {
  SectionTitle("Dificultad", info = DIFFICULTY_INFO)
  ChoiceOptions(Difficulty.entries.map { it.label }, difficulty.ordinal) { onChange(Difficulty.entries[it]) }
}

@Composable
private fun RecordLine(best: Int) {
  RecordCard(scrabbles(best).takeIf { best > 0 })
}

internal fun scrabbles(n: Int): String = "$n ${if (n == 1) "scrabble" else "scrabbles"}"

private const val SINGLE_HINT =
  "si colocas una jugada inválida, vuelven las fichas a tu atril y pierdes una vida."

private const val DIFFICULTY_INFO =
  "Fácil: todos los tableros que salgan tendrán múltiples scrabbles para colocar.\n\n" +
    "Normal: puede salirte cualquier tablero con al menos un scrabble.\n\n" +
    "Difícil: todos los tableros que salgan tendrán una sola oportunidad de scrabble."
