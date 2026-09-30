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
import app.lexico.ui.common.SectionTitle
import app.lexico.ui.common.StartButton
import app.lexico.ui.common.Stepper
import kotlin.math.roundToInt

/** Como sera una serie de "¿Cuántas recuerdas?": se juega partida tras partida hasta quedarse sin vidas. */
data class RecallConfig(
  /** Pausa entre jugada y jugada al ver la partida (la misma del tablero del inicio). */
  val intervalMs: Long = 1600,
  val wordsPerGame: Int = 10,
  val lives: Int = 3,
  /** Void (false) o single (true). */
  val single: Boolean = true,
) {
  companion object {
    val INTERVAL_MS = 500L..3000L
    val WORDS_PER_GAME = 3..12
    val LIVES = 1..5
  }
}

/** Las opciones de una serie nueva (el ritmo, las palabras por partida, las vidas y la comprobacion) y el record con ellas. */
@Composable
fun NewRecallScreen(onBack: () -> Unit, recordFor: (RecallConfig) -> Int, onStart: (RecallConfig) -> Unit) {
  val default = RecallConfig()
  var interval by remember { mutableFloatStateOf(default.intervalMs / 1000f) }
  var words by remember { mutableFloatStateOf(default.wordsPerGame.toFloat()) }
  var lives by remember { mutableIntStateOf(default.lives) }
  var single by remember { mutableStateOf(default.single) }
  val config = RecallConfig((interval * 1000).roundToInt().toLong(), words.roundToInt(), lives, single)
  Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
    Header("¿Cuántas recuerdas?", onBack)
    IntervalSetting(interval) { interval = it }
    CountSetting("Palabras por partida", words, RecallConfig.WORDS_PER_GAME) { words = it }
    SectionTitle("Vidas", info = "Cada palabra que no recuerdas cuesta una vida. Sin vidas, se acaba la serie.")
    Stepper(lives, RecallConfig.LIVES) { lives = it }
    ChallengeModeSelector(single, { single = it }, penalty = "pierdes una vida")
    RecordLine(recordFor(config))
    StartButton(enabled = true) { onStart(config) }
  }
}

/** "Tu récord con estas opciones: 7 palabras", o que aun no hay. */
@Composable
private fun RecordLine(best: Int) {
  SectionTitle(if (best > 0) "Tu récord con estas opciones: ${words(best)}" else "Aún no tienes récord con estas opciones.")
}

/** "1 palabra", "7 palabras". */
internal fun words(n: Int): String = "$n ${if (n == 1) "palabra" else "palabras"}"

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
