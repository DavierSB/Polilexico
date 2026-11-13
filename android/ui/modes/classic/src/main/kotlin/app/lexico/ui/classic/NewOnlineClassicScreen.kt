package app.lexico.ui.classic

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.lexico.ui.common.ChallengeModeSelector
import app.lexico.ui.common.Durations
import app.lexico.ui.common.FitColumn
import app.lexico.ui.common.Header

fun interface OnlineClassicCreate {
  fun create(timeMs: Long, overtimeMs: Long, single: Boolean, timed: Boolean)
}

@Composable
fun NewOnlineClassicScreen(title: String, action: String, onBack: () -> Unit, onCreate: OnlineClassicCreate) {
  val form = remember { ClassicForm().apply { timed = true } }
  val time = Durations.parse(form.time)?.takeIf { it > 0 }
  val overtime = Durations.parse(form.overtime)
  val valid = !form.timed || (time != null && overtime != null)
  FitColumn(10.dp) {
    Header(title, onBack)
    ChallengeModeSelector(form.single, { form.single = it }, singleHint = ONLINE_SINGLE_HINT)
    TimeSection(form)
    Button(modifier = Modifier.fillMaxWidth(), enabled = valid, onClick = { onCreate.create(time ?: 0, overtime ?: 0, form.single, form.timed) }) {
      Text(action)
    }
  }
}

private const val ONLINE_SINGLE_HINT = "una jugada inválida cuenta, salvo que el rival la reclame antes de jugar. Reclamar no tiene castigo."
