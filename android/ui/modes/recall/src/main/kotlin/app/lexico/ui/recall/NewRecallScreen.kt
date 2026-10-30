package app.lexico.ui.recall

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.lexico.ui.common.ChallengeModeSelector
import app.lexico.ui.common.CreateRoomButton
import app.lexico.ui.common.Durations
import app.lexico.ui.common.Header
import app.lexico.ui.common.RecordCard
import app.lexico.ui.common.RulesButton
import app.lexico.ui.common.SectionTitle
import app.lexico.ui.common.SoloOrWifiTabs
import app.lexico.ui.common.StartButton
import app.lexico.ui.common.Stepper
import app.lexico.ui.common.TimeField
import app.lexico.ui.common.WifiForm
import kotlin.math.roundToInt

data class RecallConfig(
  val intervalMs: Long = 1600,
  val wordsPerGame: Int = 10,
  val lives: Int = 3,
  val single: Boolean = true,
  val rounds: Int = 0,
  val shared: Boolean = false,
) {
  val limited: Boolean get() = lives > 0

  companion object {
    val INTERVAL_MS = 500L..3000L
    val LIVES = 1..5
  }
}

data class RecallRace(val name: String, val config: RecallConfig, val goal: Int, val roundMs: Long)

@Composable
fun NewRecallScreen(
  onBack: () -> Unit, recordFor: (RecallConfig) -> Int, onStart: (RecallConfig) -> Unit,
  onHost: (RecallRace) -> Unit, onJoin: (name: String) -> Unit,
) {
  var wifi by remember { mutableStateOf(false) }
  var rules by remember { mutableStateOf(false) }
  Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
    Header("¿Cuántas recuerdas?", onBack)
    SoloOrWifiTabs(wifi) { wifi = it }
    RulesButton { rules = true }
    if (wifi) WifiForm(onJoin) { name -> RaceSettings(name, onHost) } else SoloSettings(recordFor, onStart)
  }
  if (rules) RecallRulesDialog { rules = false }
}

@Composable
private fun SoloSettings(recordFor: (RecallConfig) -> Int, onStart: (RecallConfig) -> Unit) {
  val default = RecallConfig()
  var interval by remember { mutableFloatStateOf(default.intervalMs / 1000f) }
  var lives by remember { mutableIntStateOf(default.lives) }
  var single by remember { mutableStateOf(default.single) }
  val config = RecallConfig((interval * 1000).roundToInt().toLong(), default.wordsPerGame, lives, single)
  IntervalSetting(interval) { interval = it }
  SectionTitle("Vidas", info = "Cada palabra que no recuerdas cuesta una vida. Sin vidas, se acaba la serie.")
  Stepper(lives, RecallConfig.LIVES) { lives = it }
  ChallengeModeSelector(single, { single = it }, singleHint = SINGLE_HINT)
  RecordLine(recordFor(config))
  StartButton(enabled = true) { onStart(config) }
}

@Composable
private fun RaceSettings(name: String, onHost: (RecallRace) -> Unit) {
  val default = RecallConfig()
  var interval by remember { mutableFloatStateOf(default.intervalMs / 1000f) }
  var goal by remember { mutableIntStateOf(DEFAULT_GOAL) }
  var roundTime by remember { mutableStateOf(Durations.format(DEFAULT_ROUND_MS)) }
  var single by remember { mutableStateOf(default.single) }
  val roundMs = Durations.parse(roundTime)?.takeIf { it > 0 }
  IntervalSetting(interval) { interval = it }
  SectionTitle("Meta", info = RACE_INFO)
  Stepper(goal, GOALS) { goal = it }
  SectionTitle("Tiempo por ronda", info = "Empieza a contar al acabar la partida de muestra.")
  TimeField(roundTime, { roundTime = it }, "Tiempo")
  ChallengeModeSelector(single, { single = it }, singleHint = RACE_SINGLE_HINT)
  CreateRoomButton(enabled = name.isNotEmpty() && roundMs != null) {
    val config = RecallConfig((interval * 1000).roundToInt().toLong(), default.wordsPerGame, RACE_LIVES, single, shared = true)
    roundMs?.let { onHost(RecallRace(name, config, goal, it)) }
  }
}

@Composable
private fun RecordLine(best: Int) {
  RecordCard(words(best).takeIf { best > 0 })
}

internal fun words(n: Int): String = "$n ${if (n == 1) "palabra" else "palabras"}"

@Composable
private fun IntervalSetting(seconds: Float, onChange: (Float) -> Unit) {
  val range = RecallConfig.INTERVAL_MS
  Setting("Tiempo entre jugadas", "%.1f s".format(seconds), seconds, range.first / 1000f..range.last / 1000f,
    steps = ((range.last - range.first) / 100 - 1).toInt(), info = "Menos tiempo, más difícil recordar.", onChange = onChange)
}

@Composable
private fun Setting(
  title: String, shown: String, value: Float, range: ClosedFloatingPointRange<Float>, steps: Int,
  info: String? = null, onChange: (Float) -> Unit,
) {
  Column {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Box(Modifier.weight(1f)) { SectionTitle(title, info) }
      Text(shown, style = MaterialTheme.typography.labelLarge)
    }
    Slider(value, onChange, valueRange = range, steps = steps)
  }
}

private const val DEFAULT_GOAL = 10

private val GOALS = 1..50

private const val DEFAULT_ROUND_MS = 60_000L

private const val RACE_LIVES = 3

private const val RACE_INFO =
  "Todos ven la misma partida y tienen $RACE_LIVES vidas por ronda. La ronda termina cuando alguien llega al final, se acaba su tiempo " +
    "o nadie tiene vidas. Gana el primero en sumar estas palabras."

private const val RACE_SINGLE_HINT = "una jugada inválida cuesta una vida."

private const val SINGLE_HINT =
  "si colocas una jugada inválida, vuelven las fichas a tu atril y pierdes una vida."
