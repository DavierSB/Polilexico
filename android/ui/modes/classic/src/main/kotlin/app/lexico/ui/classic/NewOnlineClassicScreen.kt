package app.lexico.ui.classic

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.lexico.ui.common.Durations
import app.lexico.ui.common.FitColumn
import app.lexico.ui.common.Header
import app.lexico.ui.common.SectionTitle
import app.lexico.ui.common.TimeField

@Composable
fun NewOnlineClassicScreen(title: String, action: String, onBack: () -> Unit, onCreate: (timeMs: Long, overtimeMs: Long) -> Unit) {
  val form = rememberClassicForm()
  val time = Durations.parse(form.time)?.takeIf { it > 0 }
  val overtime = Durations.parse(form.overtime)
  FitColumn(10.dp) {
    Header(title, onBack)
    SectionTitle("Tiempo", info = TIME_INFO)
    TimeField(form.time, { form.time = it }, "Tiempo por jugador")
    TimeField(form.overtime, { form.overtime = it }, "Tiempo de descuento")
    Button(modifier = Modifier.fillMaxWidth(), enabled = time != null && overtime != null, onClick = { onCreate(time!!, overtime!!) }) {
      Text(action)
    }
  }
}
