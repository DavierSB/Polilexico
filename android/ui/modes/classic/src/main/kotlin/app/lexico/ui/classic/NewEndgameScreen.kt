package app.lexico.ui.classic

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Text
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
import app.lexico.ui.common.CreateRoomButton
import app.lexico.ui.common.Durations
import app.lexico.ui.common.FitColumn
import app.lexico.ui.common.Header
import app.lexico.ui.common.RadioOption
import app.lexico.ui.common.RulesButton
import app.lexico.ui.common.SettingDialog
import app.lexico.ui.common.SettingItem
import app.lexico.ui.common.SettingsList
import app.lexico.ui.common.StartButton
import app.lexico.ui.common.TwoOptions
import app.lexico.ui.common.WifiTip

private const val MAX_BAG_LIMIT = 20

private const val LEAD_STEP = 5

@Composable
fun NewEndgameScreen(onBack: () -> Unit, onStart: (EndgameConfig) -> Unit, onHost: (EndgameConfig, String) -> Unit, onJoin: (String) -> Unit) {
  val form = remember { EndgameForm() }
  val classic = form.classic
  var chosen by remember { mutableStateOf(false) }
  var rules by remember { mutableStateOf(false) }
  BackHandler(enabled = chosen) { chosen = false }
  val back = { chosen = false }
  when {
    !chosen -> RivalStep(form, onBack, { rules = true }, { onStart(form.toConfig()) }) { chosen = true }
    classic.wifi -> WifiStep(form, back, onHost = { onHost(form.toConfig(), classic.myName.trim()) }) { onJoin(classic.myName.trim()) }
    else -> SharedStep(form, back) { onStart(form.toConfig()) }
  }
  if (rules) EndgameRulesDialog { rules = false }
}

@Composable
private fun RivalStep(form: EndgameForm, onBack: () -> Unit, onRules: () -> Unit, onStart: () -> Unit, onChosen: () -> Unit) {
  Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
    Header("Finales", onBack)
    RivalTabs(form.classic.human) { form.classic.human = it; form.classic.starter = 0 }
    RulesButton(onClick = onRules)
    if (form.classic.human) HumanRival(form, onChosen) else BotSettings(form, onStart)
  }
}

@Composable
private fun BotSettings(form: EndgameForm, onStart: () -> Unit) {
  PositionSettings(form)
  ChallengeSection(form.classic)
  TimeSection(form.classic)
  StartButton(enabled = form.classic.valid, onClick = onStart)
}

@Composable
private fun HumanRival(form: EndgameForm, onChosen: () -> Unit) {
  PlayModeOptions(form.classic.wifi) { form.classic.wifi = it; form.classic.starter = 0 }
  Button(modifier = Modifier.fillMaxWidth(), onClick = onChosen) { Text("Seleccionar modo de juego") }
}

@Composable
private fun SharedStep(form: EndgameForm, onBack: () -> Unit, onStart: () -> Unit) {
  FitColumn(10.dp) {
    Header("Un solo teléfono", onBack)
    NamesSection(form.classic)
    StarterSection(form.classic)
    PositionSettings(form)
    ChallengeSection(form.classic)
    TimeSection(form.classic)
    StartButton(enabled = form.classic.valid, onClick = onStart)
  }
}

@Composable
private fun WifiStep(form: EndgameForm, onBack: () -> Unit, onHost: () -> Unit, onJoin: () -> Unit) {
  FitColumn(10.dp) {
    Header("Por wifi", onBack)
    WifiTip()
    MyNameSection(form.classic)
    TwoOptions("Crear sala", "Unirse a una sala", !form.classic.hosting) { form.classic.hosting = !it }
    if (form.classic.hosting) HostSettings(form, onHost) else JoinButton(form.classic, onJoin)
  }
}

@Composable
private fun HostSettings(form: EndgameForm, onHost: () -> Unit) {
  StarterSection(form.classic)
  PositionSettings(form)
  ChallengeSection(form.classic)
  TimeSection(form.classic)
  CreateRoomButton(enabled = form.classic.valid, onClick = onHost)
}

@Composable
private fun PositionSettings(form: EndgameForm) {
  SettingsList(listOf(bagItem(form), leadItem(form), qItem(form)))
}

private fun bagItem(form: EndgameForm): SettingItem =
  SettingItem(bagText(form.minBag, form.maxBag), bagHighlight(form.minBag, form.maxBag)) { close -> BagDialog(form, close) }

private fun leadItem(form: EndgameForm): SettingItem =
  SettingItem(leadText(form.minLead, form.maxLead, form.classic.human), leadHighlight(form.minLead, form.maxLead)) { close -> LeadDialog(form, close) }

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
  val human = form.classic.human
  SettingDialog(if (human) "Ventaja de quien sale" else "Tu ventaja al empezar", if (human) HUMAN_LEAD_TEXT else LEAD_TEXT, close, { form.minLead = min; form.maxLead = max }) {
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

private fun leadText(min: Int, max: Int, human: Boolean): String = when {
  max <= 0 -> "${goes(human)} perdiendo por ${range(-max, -min)} puntos"
  min >= 0 -> "${goes(human)} ganando por ${range(min, max)} puntos"
  else -> "Entre ir perdiendo por ${-min} y ganando por $max puntos"
}

private fun goes(human: Boolean): String = if (human) "Quien sale va" else "Vas"

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
    return EndgameConfig(minBag, maxBag, minLead, maxLead, q, c.single, c.timed, c.timeMs, c.overtimeMs, c.starter, c.players)
  }
}

private const val BAG_TEXT = "Cuántas fichas quedan en la bolsa cuando tomas el control de la partida."

private const val Q_TEXT = "Dónde está la Q cuando tomas el control de la partida."

private const val HUMAN_LEAD_TEXT = "Los puntos de quien sale menos los de su rival: negativa si va perdiendo."

private const val LEAD_TEXT = "Tus puntos menos los de Gitana al empezar: negativa si vas perdiendo."
