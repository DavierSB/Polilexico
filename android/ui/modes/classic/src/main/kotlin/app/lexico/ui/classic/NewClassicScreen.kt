package app.lexico.ui.classic

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.lexico.ui.common.ChallengeModeSelector
import app.lexico.ui.common.Header
import app.lexico.ui.common.Hint
import app.lexico.ui.common.SectionTitle
import app.lexico.ui.common.StartButton
import app.lexico.ui.common.TimeField
import app.lexico.ui.common.TwoOptions

/** Las opciones de una clasica nueva: rival, comprobacion de jugadas, tiempo y letras faltantes. */
@Composable
fun NewClassicScreen(onBack: () -> Unit, onStart: (ClassicConfig) -> Unit) {
  val form = rememberClassicForm()
  Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
    Header("Clásica", onBack)
    OpponentSection(form)
    ChallengeSection(form)
    TimeSection(form)
    UnseenSection(form)
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
  if (!form.voidOnly) return ChallengeModeSelector(form.single, { form.single = it }, penalty = "pierdes el turno")
  SectionTitle("Comprobación de jugadas")
  Hint("${alias(form.opponent)} solo juega en modo void.")
}

@Composable
internal fun TimeSection(form: ClassicForm) {
  SectionTitle("Tiempo")
  TwoOptions("Con tiempo", "Sin tiempo", !form.timed) { form.timed = !it }
  if (!form.timed) return
  TimeField(form.time, { form.time = it }, "Tiempo por jugador")
  TimeField(form.overtime, { form.overtime = it }, "Tiempo de descuento")
  Hint("Cuando se te acaba el tiempo empieza el descuento; si lo agotas también, pierdes por tiempo. No se descuentan puntos.")
}

@Composable
internal fun UnseenSection(form: ClassicForm) {
  SectionTitle("Letras faltantes")
  TwoOptions("Mostrar", "Ocultar", !form.showUnseen) { form.showUnseen = !it }
  Hint("Al tocar la bolsa: las fichas que no has visto (bolsa y atril del rival), o solo cuántas quedan.")
}
