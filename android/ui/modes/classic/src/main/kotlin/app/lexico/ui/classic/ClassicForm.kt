package app.lexico.ui.classic

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import app.lexico.ui.common.Durations

@Composable
internal fun rememberClassicForm(): ClassicForm = remember { ClassicForm() }

/** Lo que se va eligiendo en la pantalla de clasica nueva, con los tiempos tal como se escriben. */
@Stable
internal class ClassicForm {
  var opponent by mutableStateOf("HastyBot")
  var single by mutableStateOf(false)
  var timed by mutableStateOf(true)
  var time by mutableStateOf("20:00")
  var overtime by mutableStateOf("1:00")

  /** Como en Woogles, algunos bots solo juegan en modo void. */
  val voidOnly: Boolean get() = bot(opponent).voidOnly

  val valid: Boolean get() = !timed || (timeMs()?.let { it > 0 } == true && Durations.parse(overtime) != null)

  fun toConfig(): ClassicConfig =
    ClassicConfig(opponent, single && !voidOnly, timed, timeMs() ?: 0, Durations.parse(overtime) ?: 0)

  private fun timeMs(): Long? = Durations.parse(time)
}
