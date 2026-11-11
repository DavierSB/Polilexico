package app.lexico.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.foundation.layout.size
import androidx.compose.material3.IconButton
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.OutlinedButton
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

const val CHAT_GLYPH = "💬"

@Composable
fun ChatIconButton(description: String, onClick: () -> Unit) {
  IconButton(onClick, Modifier.size(40.dp)) { Text(CHAT_GLYPH, fontSize = 18.sp, modifier = Modifier.semantics { contentDescription = description }) }
}

class ChatEntry(val onOpen: () -> Unit)

val LocalChat = compositionLocalOf<ChatEntry?> { null }

data class ChatLine(val from: String, val text: String, val country: String = "", val room: Boolean = false)

enum class ContactGroup(val title: String) { ROOM("Sala"), RIVALS("Rivales"), WATCHERS("Observando"), OTHERS("Conectados") }

data class ChatContact(val name: String, val country: String, val group: ContactGroup)

const val WHOLE_ROOM = "Toda la sala"

val ROOM_CONTACT = ChatContact(WHOLE_ROOM, "", ContactGroup.ROOM)

@Composable
fun ChatPopup(lines: List<ChatLine>, onClose: () -> Unit, onReply: (ChatLine) -> Unit, onIgnoreUser: (ChatLine) -> Unit, onIgnoreAll: () -> Unit) {
  val last = lines.lastOrNull() ?: return
  Dialog(onDismissRequest = onClose) {
    Surface(shape = RoundedCornerShape(20.dp), tonalElevation = 6.dp) {
      Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(if (last.room) "${last.from} dice a la sala:" else "${last.from} dice:", Modifier.fillMaxWidth(), fontWeight = FontWeight.Bold)
        Text("“${last.text}”", Modifier.fillMaxWidth(), fontSize = 18.sp, fontStyle = FontStyle.Italic)
        if (lines.size > 1) OlderCount(lines.size - 1)
        if (last.country.isNotEmpty() || last.room) Button(onClick = { onReply(last) }, Modifier.fillMaxWidth()) { Text("Responder") }
        OutlinedButton(onClick = onClose, Modifier.fillMaxWidth()) { Text("Cerrar") }
        TextButton(onClick = { onIgnoreUser(last) }) { Text("Ignorar mensajes de ${last.from}", fontSize = 13.sp, textAlign = TextAlign.Center) }
        TextButton(onClick = onIgnoreAll) { Text("Ignorar todos los mensajes", fontSize = 13.sp, textAlign = TextAlign.Center) }
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
fun ChatComposer(
  initial: ChatContact?, contacts: List<ChatContact>, ignoring: Boolean, onSend: (ChatContact, String) -> Unit, onListen: () -> Unit,
  onHistory: () -> Unit, onClose: () -> Unit,
) {
  var text by remember { mutableStateOf("") }
  var nick by remember { mutableStateOf(initial?.name.orEmpty()) }
  var picking by remember { mutableStateOf(false) }
  val to = contacts.firstOrNull { it.name.equals(nick.trim(), ignoreCase = true) }
  AlertDialog(
    onDismissRequest = onClose, title = { ComposerTitle(onHistory) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        RecipientField(nick, to, { nick = it }) { picking = true }
        ComposerBody(text, ignoring, onListen) { text = it.take(CHAT_MAX_CHARS) }
      }
    },
    confirmButton = { Button(onClick = { to?.let { onSend(it, text.trim()) }; onClose() }, enabled = to != null && text.isNotBlank()) { Text("Enviar") } },
    dismissButton = { TextButton(onClick = onClose) { Text("Cancelar") } },
  )
  if (picking) ContactPicker(contacts, { nick = it.name; picking = false }) { picking = false }
}

@Composable
private fun ComposerTitle(onHistory: () -> Unit) {
  Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
    Text("Mensaje", Modifier.weight(1f))
    TextButton(onClick = onHistory) { Text("Historial") }
  }
}

@Composable
private fun RecipientField(nick: String, to: ChatContact?, onChange: (String) -> Unit, onPick: () -> Unit) {
  OutlinedTextField(
    nick, onChange, Modifier.fillMaxWidth(), singleLine = true, label = { Text("Para") },
    prefix = to?.takeIf { it.country.isNotEmpty() }?.let { { Text(flagOf(it.country) + " ") } },
    trailingIcon = { TextButton(onClick = onPick) { Text("▾", fontSize = 18.sp) } },
    supportingText = if (nick.isNotBlank() && to == null) ({ Text("No está conectado") }) else null,
  )
}

@Composable
private fun ContactPicker(contacts: List<ChatContact>, onPick: (ChatContact) -> Unit, onClose: () -> Unit) {
  AlertDialog(
    onDismissRequest = onClose, title = { Text("Escribir a") },
    text = {
      Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (contacts.isEmpty()) Text("No hay nadie más conectado.")
        ContactGroup.entries.forEach { group -> ContactSection(group, contacts.filter { it.group == group }, onPick) }
      }
    },
    confirmButton = { TextButton(onClick = onClose) { Text("Cerrar") } },
  )
}

@Composable
private fun ContactSection(group: ContactGroup, contacts: List<ChatContact>, onPick: (ChatContact) -> Unit) {
  if (contacts.isEmpty()) return
  Text(group.title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
  contacts.forEach { c -> if (c.group == ContactGroup.ROOM) RoomRow { onPick(c) } else PlayerRow(c.name, c.country, onClick = { onPick(c) }) {} }
}

@Composable
private fun ComposerBody(text: String, ignoring: Boolean, onListen: () -> Unit, onChange: (String) -> Unit) {
  Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
    OutlinedTextField(
      text, onChange, Modifier.fillMaxWidth(), maxLines = 3, label = { Text("Mensaje") },
      keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
      supportingText = { Text("${text.length} / $CHAT_MAX_CHARS", Modifier.fillMaxWidth(), textAlign = TextAlign.End) },
    )
    if (ignoring) TextButton(onClick = onListen) { Text("Volver a recibir mensajes") }
  }
}
