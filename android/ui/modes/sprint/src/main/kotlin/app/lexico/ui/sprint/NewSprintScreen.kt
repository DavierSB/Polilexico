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
import app.lexico.ui.common.Durations
import app.lexico.ui.common.Header
import app.lexico.ui.common.SectionTitle
import app.lexico.ui.common.StartButton
import app.lexico.ui.common.Stepper
import app.lexico.ui.common.TimeField

const val DEFAULT_TOTAL_MS = 300_000L

data class SprintConfig(val totalMs: Long = DEFAULT_TOTAL_MS, val lives: Int = DEFAULT_LIVES, val single: Boolean = true) {
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
  val config = Durations.parse(total)?.takeIf { it > 0 }?.let { SprintConfig(it, lives, single) }
  Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
    Header("Scrabble Sprint", onBack)
    SectionTitle("Tiempo", info = "El reloj solo corre mientras buscas el scrabble de una mano. Cuando se agota, se acaba la serie.")
    TimeField(total, { total = it }, "Tiempo de la serie")
    SectionTitle("Vidas", info = "Rendirte en una mano cuesta una vida.")
    Stepper(lives, SprintConfig.LIVES) { lives = it }
    ChallengeModeSelector(single, { single = it }, penalty = "pierdes una vida")
    config?.let { RecordLine(recordFor(it)) }
    StartButton(enabled = config != null) { config?.let(onStart) }
  }
}

@Composable
private fun RecordLine(best: Int) {
  SectionTitle(if (best > 0) "Tu récord con estas opciones: ${hands(best)}" else "Aún no tienes récord con estas opciones.")
}

internal fun hands(n: Int): String = "$n ${if (n == 1) "mano" else "manos"}"
