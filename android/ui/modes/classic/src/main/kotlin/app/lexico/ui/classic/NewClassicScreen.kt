package app.lexico.ui.classic

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.lexico.ui.common.ChallengeModeSelector
import app.lexico.ui.common.ChoiceOptions
import app.lexico.ui.common.FitColumn
import app.lexico.ui.common.Header
import app.lexico.ui.common.SectionTitle
import app.lexico.ui.common.StartButton
import app.lexico.ui.common.TimeField
import app.lexico.ui.common.TwoOptions

@Composable
fun NewClassicScreen(onBack: () -> Unit, onStart: (ClassicConfig) -> Unit) {
  val form = rememberClassicForm()
  var chosen by remember { mutableStateOf(false) }
  BackHandler(enabled = chosen) { chosen = false }
  if (chosen) SettingsStep(form, onBack = { chosen = false }) { onStart(form.toConfig()) }
  else OpponentStep(form, onBack) { chosen = true }
}

@Composable
private fun OpponentStep(form: ClassicForm, onBack: () -> Unit, onChosen: () -> Unit) {
  FitColumn(10.dp) {
    Header("Clásica", onBack)
    SectionTitle("Rival")
    BOTS.forEach { b -> BotOption(b, selected = b.name == form.opponent) { form.opponent = b.name } }
    Button(modifier = Modifier.fillMaxWidth(), onClick = onChosen) { Text("Seleccionar rival") }
  }
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
    StartButton(enabled = form.valid, onClick = onStart)
  }
}

@Composable
private fun StarterSection(form: ClassicForm) {
  SectionTitle("Salidor")
  ChoiceOptions(listOf("Cualquiera", "Tú", alias(form.opponent)), form.starter) { form.starter = it }
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

private const val TIME_INFO =
  "Cuando se te acaba el tiempo empieza el descuento. Por cada minuto de descuento que empieces se te restan 10 puntos; " +
    "si agotas también el descuento, pierdes por tiempo."

private const val SINGLE_HINT =
  "si colocas una jugada inválida, obtienes 0 puntos, vuelven las fichas a tu atril y pierdes el turno."
