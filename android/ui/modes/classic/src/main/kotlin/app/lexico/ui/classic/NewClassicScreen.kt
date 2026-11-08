package app.lexico.ui.classic

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.lexico.ui.common.ButtonText
import app.lexico.ui.common.ChallengeModeSelector
import app.lexico.ui.common.ChoiceOptions
import app.lexico.ui.common.CreateRoomButton
import app.lexico.ui.common.FitColumn
import app.lexico.ui.common.OnlineTab
import app.lexico.ui.common.SavedButtons
import app.lexico.ui.common.SavedShortcuts
import app.lexico.ui.common.SwitchPrompt
import app.lexico.ui.common.Header
import app.lexico.ui.common.SectionTitle
import app.lexico.ui.common.StartButton
import app.lexico.ui.common.StudyModeSelector
import app.lexico.ui.common.TimeField
import app.lexico.ui.common.TwoOptions
import app.lexico.ui.common.WifiTip

@Composable
fun NewClassicScreen(
  online: OnlineTab, saved: SavedShortcuts, onBack: () -> Unit, onStart: (ClassicConfig) -> Unit, onHost: (ClassicConfig, String) -> Unit, onJoin: (String) -> Unit,
) {
  val form = rememberClassicForm()
  var chosen by remember { mutableStateOf(false) }
  var tab by remember { mutableStateOf(if (online.preferred) RivalTab.ONLINE else RivalTab.LOCAL) }
  BackHandler(enabled = chosen) { chosen = false }
  val back = { chosen = false }
  when {
    !chosen && tab == RivalTab.ONLINE -> OnlineStep(online, onBack) { tab = it }
    !chosen -> LocalStep(form, saved, onBack, { tab = it }) { chosen = true }
    !form.human -> SettingsStep(form, back) { onStart(form.toConfig()) }
    form.wifi -> WifiStep(form, back, onHost = { onHost(form.toConfig(), form.myName.trim()) }) { onJoin(form.myName.trim()) }
    else -> SharedStep(form, back) { onStart(form.toConfig()) }
  }
}

@Composable
private fun LocalStep(form: ClassicForm, saved: SavedShortcuts, onBack: () -> Unit, onTab: (RivalTab) -> Unit, onChosen: () -> Unit) {
  val switch = { form.human = !form.human; form.starter = 0 }
  Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
    Header("Clásica", onBack)
    RivalTabs(RivalTab.LOCAL, onTab)
    Box(Modifier.weight(1f)) {
      FitColumn(10.dp) {
        if (form.human) HumanRival(form) else BotRival(form)
        SwitchPrompt(if (form.human) "¿O prefieres jugar contra un bot?" else "¿O prefieres jugar contra una persona?", switch)
      }
    }
    Button(modifier = Modifier.fillMaxWidth(), onClick = onChosen) { ButtonText(if (form.human) "Seleccionar modo de juego" else "Seleccionar rival") }
    SavedButtons(saved)
  }
}

@Composable
private fun OnlineStep(online: OnlineTab, onBack: () -> Unit, onTab: (RivalTab) -> Unit) {
  Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
    Header("Clásica", onBack)
    RivalTabs(RivalTab.ONLINE, onTab)
    online.content()
  }
}

@Composable
private fun BotRival(form: ClassicForm) {
  BOTS.forEach { b -> BotOption(b, selected = b.name == form.opponent) { form.opponent = b.name } }
}

@Composable
private fun HumanRival(form: ClassicForm) {
  PlayModeOptions(form.wifi) { form.wifi = it; form.starter = 0 }
}

@Composable
private fun SharedStep(form: ClassicForm, onBack: () -> Unit, onStart: () -> Unit) {
  FitColumn(10.dp) {
    Header("Compartir teléfono", onBack)
    NamesSection(form)
    StarterSection(form)
    ChallengeSection(form)
    TimeSection(form)
    StartButton(enabled = form.valid, onClick = onStart)
  }
}

@Composable
private fun WifiStep(form: ClassicForm, onBack: () -> Unit, onHost: () -> Unit, onJoin: () -> Unit) {
  FitColumn(10.dp) {
    Header("Por wifi", onBack)
    WifiTip()
    MyNameSection(form)
    TwoOptions("Crear sala", "Unirse a una sala", !form.hosting) { form.hosting = !it }
    if (form.hosting) HostSettings(form, onHost) else JoinButton(form, onJoin)
  }
}

@Composable
private fun HostSettings(form: ClassicForm, onHost: () -> Unit) {
  StarterSection(form)
  ChallengeSection(form)
  TimeSection(form)
  CreateRoomButton(enabled = form.valid, onClick = onHost)
}

@Composable
internal fun JoinButton(form: ClassicForm, onJoin: () -> Unit) {
  Button(modifier = Modifier.fillMaxWidth(), enabled = form.nameValid(), onClick = onJoin) { Text("Buscar salas") }
}

@Composable
private fun SettingsStep(form: ClassicForm, onBack: () -> Unit, onStart: () -> Unit) {
  FitColumn(10.dp) {
    Header("Clásica", onBack)
    SectionTitle("Rival")
    BotOption(bot(form.opponent), selected = true, select = onBack)
    StarterSection(form)
    ChallengeSection(form)
    TimeSection(form)
    StudyModeSelector(form.study) { form.study = it }
    StartButton(enabled = form.valid, onClick = onStart)
  }
}

@Composable
internal fun StarterSection(form: ClassicForm) {
  SectionTitle("Salidor")
  ChoiceOptions(form.starters, form.starter) { form.starter = it }
}

@Composable
internal fun ChallengeSection(form: ClassicForm) {
  if (!form.voidOnly) return ChallengeModeSelector(form.single, { form.single = it }, singleHint = SINGLE_HINT)
  SectionTitle("Comprobación de jugadas", info = "${alias(form.opponent)} solo juega en modo void: una jugada con palabras no válidas se rechaza y puedes volver a intentarlo.")
  Text("Void")
}

@Composable
internal fun TimeSection(form: ClassicForm) {
  SectionTitle("Tiempo", info = TIME_INFO)
  TwoOptions("Con tiempo", "Sin tiempo", !form.timed) { form.timed = !it }
  if (!form.timed) return
  TimeField(form.time, { form.time = it }, "Tiempo por jugador")
  TimeField(form.overtime, { form.overtime = it }, "Tiempo de descuento")
}

internal const val TIME_INFO =
  "Cuando se te acaba el tiempo empieza el descuento. Por cada minuto de descuento que empieces se te restan 10 puntos; " +
    "si agotas también el descuento, pierdes por tiempo."

internal const val SINGLE_HINT =
  "si colocas una jugada inválida, obtienes 0 puntos, vuelven las fichas a tu atril y pierdes el turno."
