package app.lexico.ui.classic

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.lexico.ui.common.ChallengeModeSelector
import app.lexico.ui.common.Header
import app.lexico.ui.common.SectionTitle
import app.lexico.ui.common.StartButton
import app.lexico.ui.common.TimeField
import app.lexico.ui.common.TwoOptions

@Composable
fun NewClassicScreen(onBack: () -> Unit, onStart: (ClassicConfig) -> Unit) {
  val form = rememberClassicForm()
  Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
    Header("Clásica", onBack)
    OpponentSection(form)
    ChallengeSection(form)
    TimeSection(form)
    StartButton(enabled = form.valid) { onStart(form.toConfig()) }
  }
}

@Composable
private fun OpponentSection(form: ClassicForm) {
  SectionTitle("Rival")
  BOTS.forEach { b -> BotOption(b, selected = b.name == form.opponent) { form.opponent = b.name } }
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
