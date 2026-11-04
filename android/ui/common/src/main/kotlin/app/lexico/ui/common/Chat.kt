package app.lexico.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

const val CHAT_MAX_CHARS = 140

class ChatEntry(val onOpen: () -> Unit)

val LocalChat = compositionLocalOf<ChatEntry?> { null }

data class ChatLine(val from: String, val text: String)

@Composable
fun ChatPopup(lines: List<ChatLine>, onClose: () -> Unit, onIgnore: () -> Unit) {
  val last = lines.lastOrNull() ?: return
  Dialog(onDismissRequest = onClose) {
    Surface(shape = RoundedCornerShape(20.dp), tonalElevation = 6.dp) {
      Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("${last.from} dice:", Modifier.fillMaxWidth(), fontWeight = FontWeight.Bold)
        Text("“${last.text}”", Modifier.fillMaxWidth(), fontSize = 18.sp, fontStyle = FontStyle.Italic)
        if (lines.size > 1) OlderCount(lines.size - 1)
        Button(onClick = onClose, Modifier.fillMaxWidth()) { Text("Cerrar") }
        TextButton(onClick = onIgnore) { Text("Ignorar mensajes de ${last.from}", fontSize = 13.sp, textAlign = TextAlign.Center) }
      }
    }
  }
}

@Composable
private fun OlderCount(n: Int) {
  Text(
    if (n == 1) "+1 anterior" else "+$n anteriores", Modifier.fillMaxWidth(),
    style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
  )
}

@Composable
fun ChatComposer(to: String, waitingAnswer: Boolean, ignoring: Boolean, onSend: (String) -> Unit, onListen: () -> Unit, onClose: () -> Unit) {
  var text by remember { mutableStateOf("") }
  AlertDialog(
    onDismissRequest = onClose, title = { Text("Mensaje para $to") },
    text = { ComposerBody(text, ignoring, onListen) { text = it.take(CHAT_MAX_CHARS) } },
    confirmButton = {
      Button(onClick = { onSend(text.trim()); onClose() }, enabled = !waitingAnswer && text.isNotBlank()) {
        Text(if (waitingAnswer) "Espera respuesta" else "Enviar")
      }
    },
    dismissButton = { TextButton(onClick = onClose) { Text("Cancelar") } },
  )
}

@Composable
private fun ComposerBody(text: String, ignoring: Boolean, onListen: () -> Unit, onChange: (String) -> Unit) {
  Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
    OutlinedTextField(
      text, onChange, Modifier.fillMaxWidth(), maxLines = 3,
      keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
      supportingText = { Text("${text.length} / $CHAT_MAX_CHARS", Modifier.fillMaxWidth(), textAlign = TextAlign.End) },
    )
    if (ignoring) TextButton(onClick = onListen) { Text("Volver a ver sus mensajes") }
  }
}
