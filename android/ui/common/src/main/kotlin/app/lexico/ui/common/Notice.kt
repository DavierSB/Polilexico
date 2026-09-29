package app.lexico.ui.common

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.text.font.FontWeight
import kotlinx.coroutines.delay

/** Lo que se le dice al jugador cuando las fichas que puso no forman una jugada. */
const val NOT_IN_A_LINE = "Las fichas tienen que formar una sola línea continua."

/** Cuanto se ve un aviso antes de borrarse solo. */
private const val NOTICE_MS = 3_000L

/**
 * El aviso de una partida: empieza en [notice] (lo que dice el juego) y la pantalla puede poner
 * otro (fichas fuera de linea...). Cualquiera de los dos se borra solo a los pocos segundos.
 */
@Composable
fun rememberNotice(notice: String?): MutableState<String?> {
  val message = remember(notice) { mutableStateOf(notice) }
  LaunchedEffect(message, message.value) {
    if (message.value != null) {
      delay(NOTICE_MS)
      message.value = null
    }
  }
  return message
}

/** Un aviso para el jugador (una jugada rechazada...), en el color de error. */
@Composable
fun Notice(text: String, bold: Boolean = false) {
  Text(
    text, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall,
    fontWeight = if (bold) FontWeight.Bold else null,
  )
}
