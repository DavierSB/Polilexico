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
import app.lexico.ui.common.RadioOption
import app.lexico.ui.common.RulesButton
import app.lexico.ui.common.SettingDialog
import app.lexico.ui.common.SettingItem
import app.lexico.ui.common.SettingsList
import app.lexico.ui.common.StartButton

private const val MAX_BAG_LIMIT = 20

private const val LEAD_STEP = 5

@Composable
fun NewEndgameScreen(onBack: () -> Unit, onStart: (EndgameConfig) -> Unit) {
  val form = remember { EndgameForm() }
  var rules by remember { mutableStateOf(false) }
  Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
    Header("Finales", onBack)
    RulesButton { rules = true }
    SettingsList(listOf(bagItem(form), leadItem(form), qItem(form)))
    ChallengeSection(form.classic)
    TimeSection(form.classic)
    StartButton(enabled = form.classic.valid) { onStart(form.toConfig()) }
  }
  if (rules) EndgameRulesDialog { rules = false }
}

private fun bagItem(form: EndgameForm): SettingItem =
  SettingItem(bagText(form.minBag, form.maxBag), bagHighlight(form.minBag, form.maxBag)) { close -> BagDialog(form, close) }

private fun leadItem(form: EndgameForm): SettingItem =
  SettingItem(leadText(form.minLead, form.maxLead), leadHighlight(form.minLead, form.maxLead)) { close -> LeadDialog(form, close) }

private fun qItem(form: EndgameForm): SettingItem =
  SettingItem("La Q ${form.q.summary}", form.q.summary) { close -> QDialog(form, close) }

@Composable
private fun QDialog(form: EndgameForm, close: () -> Unit) {
  var draft by remember { mutableStateOf(form.q) }
  SettingDialog("La Q", Q_TEXT, close, { form.q = draft }) {
    QPlace.entries.forEach { RadioOption(it.option, draft == it) { draft = it } }
  }
}

@Composable
private fun BagDialog(form: EndgameForm, close: () -> Unit) {
  var min by remember { mutableIntStateOf(form.minBag) }
  var max by remember { mutableIntStateOf(form.maxBag) }
  SettingDialog("Fichas en la bolsa", BAG_TEXT, close, { form.minBag = min; form.maxBag = max }) {
    RangeBar(min, max, 0..MAX_BAG_LIMIT, 1, "Mínimo" to "Máximo", { "$it" }) { a, b -> min = a; max = b }
  }
}

@Composable
private fun LeadDialog(form: EndgameForm, close: () -> Unit) {
  var min by remember { mutableIntStateOf(form.minLead) }
  var max by remember { mutableIntStateOf(form.maxLead) }
  SettingDialog("Tu ventaja al empezar", LEAD_TEXT, close, { form.minLead = min; form.maxLead = max }) {
    RangeBar(min, max, -LEAD_LIMIT..LEAD_LIMIT, LEAD_STEP, "Mínima" to "Máxima", ::signed) { a, b -> min = a; max = b }
  }
}

private fun bagText(min: Int, max: Int): String = when {
  min != max -> "Entre $min y $max fichas en la bolsa"
  max == 0 -> "Con la bolsa vacía"
  else -> "${tiles(max)} en la bolsa"
}

private fun bagHighlight(min: Int, max: Int): String = when {
  min != max -> "$min y $max fichas"
  max == 0 -> "bolsa vacía"
  else -> tiles(max)
}

private fun tiles(n: Int): String = if (n == 1) "1 ficha" else "$n fichas"

private fun leadText(min: Int, max: Int): String = when {
  max <= 0 -> "Vas perdiendo por ${range(-max, -min)} puntos"
  min >= 0 -> "Vas ganando por ${range(min, max)} puntos"
  else -> "Entre ir perdiendo por ${-min} y ganando por $max puntos"
}

private fun leadHighlight(min: Int, max: Int): String? = when {
  max <= 0 -> "${range(-max, -min)} puntos"
  min >= 0 -> "${range(min, max)} puntos"
  else -> null
}

private fun range(from: Int, to: Int): String = if (from == to) "$from" else "entre $from y $to"

@Stable
private class EndgameForm {
  private val defaults = EndgameConfig()
  var minBag by mutableIntStateOf(defaults.minBag)
  var maxBag by mutableIntStateOf(defaults.maxBag)
  var minLead by mutableIntStateOf(defaults.minLead)
  var maxLead by mutableIntStateOf(defaults.maxLead)
  var q by mutableStateOf(defaults.q)
  val classic = ClassicForm().apply {
    timed = defaults.timed
    time = Durations.format(defaults.timeMs)
    overtime = Durations.format(defaults.overtimeMs)
  }

  fun toConfig(): EndgameConfig {
    val c = classic.toConfig()
    return EndgameConfig(minBag, maxBag, minLead, maxLead, q, c.single, c.timed, c.timeMs, c.overtimeMs)
  }
}

private const val BAG_TEXT = "Cuántas fichas quedan en la bolsa cuando tomas el control de la partida."

private const val Q_TEXT = "Dónde está la Q cuando tomas el control de la partida."

private const val LEAD_TEXT = "Tus puntos menos los de Gitana al empezar: negativa si vas perdiendo."
