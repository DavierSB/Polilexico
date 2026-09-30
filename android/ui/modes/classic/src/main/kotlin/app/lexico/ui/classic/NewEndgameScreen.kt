package app.lexico.ui.classic

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.lexico.ui.common.Durations
import app.lexico.ui.common.Header
import app.lexico.ui.common.SectionTitle
import app.lexico.ui.common.StartButton
import app.lexico.ui.common.Stepper

private const val MAX_BAG_LIMIT = 50

@Composable
fun NewEndgameScreen(onBack: () -> Unit, onStart: (EndgameConfig) -> Unit) {
  val form = remember { EndgameForm() }
  Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
    Header("Finales", onBack)
    BagSection(form)
    LeadSection(form)
    ChallengeSection(form.classic)
    TimeSection(form.classic)
    StartButton(enabled = form.classic.valid) { onStart(form.toConfig()) }
  }
}

@Composable
private fun BagSection(form: EndgameForm) {
  SectionTitle("Fichas en la bolsa", info = "Empiezas la primera vez que en la bolsa quedan ${form.maxBag} fichas o menos.")
  Stepper(form.maxBag, 0..MAX_BAG_LIMIT) { form.maxBag = it }
}

@Composable
private fun LeadSection(form: EndgameForm) {
  SectionTitle("Tu ventaja al empezar", info = "Tus puntos menos los de Gitana: negativa si vas perdiendo.")
  LeadRangeBar(form.minLead, form.maxLead, LEAD_LIMIT) { min, max -> form.minLead = min; form.maxLead = max }
}

@Stable
private class EndgameForm {
  private val defaults = EndgameConfig()
  var maxBag by mutableIntStateOf(defaults.maxBag)
  var minLead by mutableIntStateOf(defaults.minLead)
  var maxLead by mutableIntStateOf(defaults.maxLead)
  val classic = ClassicForm().apply {
    time = Durations.format(defaults.timeMs)
    overtime = Durations.format(defaults.overtimeMs)
  }

  fun toConfig(): EndgameConfig {
    val c = classic.toConfig()
    return EndgameConfig(maxBag, minLead, maxLead, c.single, c.timed, c.timeMs, c.overtimeMs)
  }
}
