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

@Stable
internal class ClassicForm {
  var opponent by mutableStateOf("HastyBot")
  var starter by mutableStateOf(0)
  var single by mutableStateOf(true)
  var timed by mutableStateOf(false)
  var time by mutableStateOf(Durations.format(ClassicConfig().timeMs))
  var overtime by mutableStateOf(Durations.format(ClassicConfig().overtimeMs))
  var study by mutableStateOf(false)
  var human by mutableStateOf(false)
  var first by mutableStateOf("Jugador A")
  var second by mutableStateOf("Jugador B")
  var wifi by mutableStateOf(true)
  var hosting by mutableStateOf(true)
  var myName by mutableStateOf("")

  val voidOnly: Boolean get() = !human && bot(opponent).voidOnly

  val valid: Boolean get() = timeValid() && when {
    !human -> true
    wifi -> nameValid()
    else -> namesValid()
  }

  val starters: List<String> get() = listOf("Cualquiera") + when {
    !human -> listOf("Tú", alias(opponent))
    wifi -> listOf("Yo", "Rival")
    else -> listOf(first.trim(), second.trim())
  }

  fun toConfig(): ClassicConfig = ClassicConfig(
    opponent, starter, single && !voidOnly, timed, timeMs() ?: 0, Durations.parse(overtime) ?: 0, study && !human,
    players = players(),
  )

  fun nameValid(): Boolean = myName.isNotBlank() && !isBot(myName.trim())

  private fun players(): Pair<String, String>? = when {
    !human -> null
    wifi -> myName.trim() to ""
    else -> first.trim() to second.trim()
  }

  fun nameError(name: String, other: String): Boolean = name.isBlank() || isBot(name.trim()) || sameName(name, other)

  private fun namesValid(): Boolean = !nameError(first, second) && !nameError(second, first)

  private fun sameName(a: String, b: String): Boolean = a.trim().equals(b.trim(), ignoreCase = true)

  private fun timeValid(): Boolean = !timed || (timeMs()?.let { it > 0 } == true && Durations.parse(overtime) != null)

  private fun timeMs(): Long? = Durations.parse(time)
}
