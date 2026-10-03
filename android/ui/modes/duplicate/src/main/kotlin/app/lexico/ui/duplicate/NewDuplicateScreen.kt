package app.lexico.ui.duplicate

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
import app.lexico.ui.common.Durations
import app.lexico.ui.common.Header
import app.lexico.ui.common.RecordCard
import app.lexico.ui.common.RulesButton
import app.lexico.ui.common.SectionTitle
import app.lexico.ui.common.StartButton
import app.lexico.ui.common.Stepper
import app.lexico.ui.common.TimeField
import app.lexico.ui.common.TwoOptions
import java.util.Locale

const val DEFAULT_TURN_MS = 180_000L
const val PRESET_ROUNDS = 12

data class DuplicateConfig(
  val single: Boolean = true,
  val turnMs: Long = DEFAULT_TURN_MS,
  val rounds: Int = 0,
)

@Composable
fun NewDuplicateScreen(onBack: () -> Unit, recordFor: (DuplicateConfig) -> Int, onStart: (DuplicateConfig) -> Unit) {
  var single by remember { mutableStateOf(true) }
  var turn by remember { mutableStateOf(Durations.format(DEFAULT_TURN_MS)) }
  var fixed by remember { mutableStateOf(false) }
  var rounds by remember { mutableIntStateOf(PRESET_ROUNDS) }
  var rules by remember { mutableStateOf(false) }
  val turnMs = Durations.parse(turn)?.takeIf { it > 0 }
  val config = turnMs?.let { DuplicateConfig(single, it, if (fixed) rounds else 0) }
  Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
    Header("Duplicada", onBack)
    RulesButton { rules = true }
    RoundsSection(fixed, rounds, { fixed = it }, { rounds = it })
    ChallengeModeSelector(single, { single = it }, singleHint = SINGLE_HINT)
    SectionTitle("Tiempo", info = "Si no juegas a tiempo, 0 puntos ese turno.")
    TimeField(turn, { turn = it }, "Tiempo por turno")
    config?.let { RecordLine(recordFor(it)) }
    StartButton(enabled = config != null) { config?.let(onStart) }
  }
  if (rules) DuplicateRulesDialog { rules = false }
}

@Composable
private fun RoundsSection(fixed: Boolean, rounds: Int, onFixed: (Boolean) -> Unit, onRounds: (Int) -> Unit) {
  SectionTitle("Rondas", info = ROUNDS_TEXT)
  TwoOptions("Bolsa completa", "Nº de rondas fijo", fixed, onFixed)
  if (fixed) Stepper(rounds, ROUNDS, onRounds)
}

@Composable
private fun RecordLine(best: Int) {
  RecordCard(efficiencyText(best).takeIf { best > 0 })
}

internal fun efficiencyText(tenths: Int): String = String.format(Locale("es"), "%.1f %%", tenths / 10.0)

private val ROUNDS = 1..40

private const val ROUNDS_TEXT =
  "Cuántas rondas se juegan. A bolsa completa, la partida sigue hasta que ya no se puede formar un atril válido."

private const val SINGLE_HINT =
  "si colocas una jugada inválida, obtienes 0 puntos en esa ronda."
