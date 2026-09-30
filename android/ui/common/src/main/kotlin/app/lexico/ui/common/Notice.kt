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

const val NOT_IN_A_LINE = "Las fichas tienen que formar una sola línea continua."

private const val NOTICE_MS = 3_000L

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

@Composable
fun Notice(text: String, bold: Boolean = false) {
  Text(
    text, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall,
    fontWeight = if (bold) FontWeight.Bold else null,
  )
}
