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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.lexico.ui.common.Header
import app.lexico.ui.common.SectionTitle
import app.lexico.ui.common.StartButton
import kotlin.math.roundToInt

/** Como sera una serie de "¿Cuántas recuerdas?". */
data class RecallConfig(
  /** Pausa entre jugada y jugada al ver la partida (la misma del tablero del inicio). */
  val intervalMs: Long = 1600,
  val wordsPerRound: Int = 10,
  val rounds: Int = 3,
) {
  companion object {
    val INTERVAL_MS = 500L..3000L
    val WORDS_PER_ROUND = 3..12
    val ROUNDS = 1..10
  }
}

/** Las opciones de una serie nueva: el ritmo de la partida, las palabras por ronda y las rondas. */
@Composable
fun NewRecallScreen(onBack: () -> Unit, onStart: (RecallConfig) -> Unit) {
  val default = RecallConfig()
  var interval by remember { mutableFloatStateOf(default.intervalMs / 1000f) }
  var words by remember { mutableFloatStateOf(default.wordsPerRound.toFloat()) }
  var rounds by remember { mutableFloatStateOf(default.rounds.toFloat()) }
  Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
    Header("¿Cuántas recuerdas?", onBack)
    IntervalSetting(interval) { interval = it }
    CountSetting("Palabras por ronda", words, RecallConfig.WORDS_PER_ROUND) { words = it }
    CountSetting("Rondas", rounds, RecallConfig.ROUNDS) { rounds = it }
    StartButton(enabled = true) { onStart(RecallConfig((interval * 1000).roundToInt().toLong(), words.roundToInt(), rounds.roundToInt())) }
  }
}

/** El tiempo entre jugadas, en segundos con un decimal. */
@Composable
private fun IntervalSetting(seconds: Float, onChange: (Float) -> Unit) {
  val range = RecallConfig.INTERVAL_MS
  Setting("Tiempo entre jugadas", "%.1f s".format(seconds), seconds, range.first / 1000f..range.last / 1000f,
    steps = ((range.last - range.first) / 100 - 1).toInt(), info = "Menos tiempo, más difícil recordar.", onChange = onChange)
}

/** Una cantidad entera dentro de `range`. */
@Composable
private fun CountSetting(title: String, value: Float, range: IntRange, onChange: (Float) -> Unit) {
  Setting(title, "${value.roundToInt()}", value, range.range(), steps = range.steps(), onChange = onChange)
}

/** Un valor con su deslizador: el titulo (con su ⓘ, si hay `info`) y, a la derecha, el valor elegido. */
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

private fun IntRange.range(): ClosedFloatingPointRange<Float> = first.toFloat()..last.toFloat()

/** Posiciones intermedias del deslizador para que se detenga en cada entero. */
private fun IntRange.steps(): Int = last - first - 1
