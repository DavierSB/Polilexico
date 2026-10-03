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
import app.lexico.ui.common.Header
import app.lexico.ui.common.RecordCard
import app.lexico.ui.common.RulesButton
import app.lexico.ui.common.SectionTitle
import app.lexico.ui.common.StartButton
import app.lexico.ui.common.Stepper
import kotlin.math.roundToInt

data class RecallConfig(
  val intervalMs: Long = 1600,
  val wordsPerGame: Int = 10,
  val lives: Int = 3,
  val single: Boolean = true,
) {
  companion object {
    val INTERVAL_MS = 500L..3000L
    val LIVES = 1..5
  }
}

@Composable
fun NewRecallScreen(onBack: () -> Unit, recordFor: (RecallConfig) -> Int, onStart: (RecallConfig) -> Unit) {
  val default = RecallConfig()
  var interval by remember { mutableFloatStateOf(default.intervalMs / 1000f) }
  var lives by remember { mutableIntStateOf(default.lives) }
  var single by remember { mutableStateOf(default.single) }
  val config = RecallConfig((interval * 1000).roundToInt().toLong(), default.wordsPerGame, lives, single)
  var rules by remember { mutableStateOf(false) }
  Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
    Header("¿Cuántas recuerdas?", onBack)
    RulesButton { rules = true }
    IntervalSetting(interval) { interval = it }
    SectionTitle("Vidas", info = "Cada palabra que no recuerdas cuesta una vida. Sin vidas, se acaba la serie.")
    Stepper(lives, RecallConfig.LIVES) { lives = it }
    ChallengeModeSelector(single, { single = it }, singleHint = SINGLE_HINT)
    RecordLine(recordFor(config))
    StartButton(enabled = true) { onStart(config) }
  }
  if (rules) RecallRulesDialog { rules = false }
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

private const val SINGLE_HINT =
  "si colocas una jugada inválida, vuelven las fichas a tu atril y pierdes una vida."
