package app.lexico.ui.duplicate

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.lexico.ui.common.Header
import app.lexico.ui.common.TimeField
import app.lexico.ui.common.ChallengeModeSelector
import app.lexico.ui.common.Durations
import app.lexico.ui.common.Hint
import app.lexico.ui.common.SectionTitle
import app.lexico.ui.common.StartButton

/** Tiempo por turno de la duplicada, como en ./duplicate.sh: 3:20. */
const val DEFAULT_TURN_MS = 200_000L

/** Como sera una duplicada nueva. */
data class DuplicateConfig(
  /** Void (false) o single (true). */
  val single: Boolean = false,
  val turnMs: Long = DEFAULT_TURN_MS,
)

/** Las opciones de una duplicada nueva: comprobacion de jugadas y tiempo por turno. */
@Composable
fun NewDuplicateScreen(onBack: () -> Unit, onStart: (DuplicateConfig) -> Unit) {
  var single by remember { mutableStateOf(false) }
  var turn by remember { mutableStateOf(Durations.format(DEFAULT_TURN_MS)) }
  val turnMs = Durations.parse(turn)?.takeIf { it > 0 }
  Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
    Header("Duplicada", onBack)
    ChallengeModeSelector(single, { single = it }, penalty = "pierdes el turno (0 puntos)")
    SectionTitle("Tiempo")
    TimeField(turn, { turn = it }, "Tiempo por turno")
    Hint("Si no juegas a tiempo, 0 puntos ese turno.")
    StartButton(enabled = turnMs != null) { onStart(DuplicateConfig(single, turnMs ?: DEFAULT_TURN_MS)) }
  }
}
