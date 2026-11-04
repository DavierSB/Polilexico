package app.lexico.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

data class ProfileData(val name: String, val country: String, val email: String)

class ProfileActions(
  val back: () -> Unit, val save: (ProfileData, password: String) -> Unit, val changePhoto: () -> Unit, val removePhoto: () -> Unit,
  val stats: () -> Unit, val logOut: () -> Unit,
)

@Composable
fun OnlineProfileScreen(data: ProfileData, hasPhoto: Boolean, busy: Boolean, notice: String?, editing: Boolean, onEdit: (Boolean) -> Unit, actions: ProfileActions) {
  FitColumn(12.dp) {
    Header(if (editing) "Editar perfil" else "Mi perfil", { if (editing) onEdit(false) else actions.back() })
    BigIdentity(data)
    if (editing) EditFields(data, hasPhoto, busy, actions) { onEdit(false) } else ProfileView(data, actions) { onEdit(true) }
    notice?.let { Text(it, color = MaterialTheme.colorScheme.error) }
  }
}

@Composable
private fun BigIdentity(data: ProfileData) {
  Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
    PersonAvatar(data.name, data.country, highlighted = true, size = 120.dp)
    Text("${data.name}  ${flagOf(data.country)}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
  }
}

@Composable
private fun ProfileView(data: ProfileData, actions: ProfileActions, onEdit: () -> Unit) {
  var leaving by remember { mutableStateOf(false) }
  DataCard(data)
  Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    OutlinedButton(onClick = actions.stats, Modifier.weight(1f)) { Text("Estadísticas") }
    Button(onClick = onEdit, Modifier.weight(1f)) { Text("Editar") }
  }
  TextButton(onClick = { leaving = true }, Modifier.fillMaxWidth()) { Text("Cerrar sesión") }
  if (leaving) LogOutDialog({ leaving = false; actions.logOut() }) { leaving = false }
}

@Composable
private fun DataCard(data: ProfileData) {
  Column(
    Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp)).padding(14.dp),
    verticalArrangement = Arrangement.spacedBy(6.dp),
  ) {
    DataRow("Usuario", data.name)
    DataRow("País", "${flagOf(data.country)}  ${COUNTRIES.find { it.code == data.country }?.name ?: data.country}")
    DataRow("Correo", data.email.ifEmpty { "Sin correo" })
    DataRow("Contraseña", "••••••")
  }
}

@Composable
private fun DataRow(label: String, value: String) {
  Row(Modifier.fillMaxWidth()) {
    Text(label, Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
    Text(value, fontWeight = FontWeight.Bold, maxLines = 1)
  }
}

@Composable
private fun EditFields(data: ProfileData, hasPhoto: Boolean, busy: Boolean, actions: ProfileActions, onCancel: () -> Unit) {
  var edited by remember(data) { mutableStateOf(data) }
  var password by remember { mutableStateOf("") }
  PhotoButtons(hasPhoto, actions)
  OutlinedTextField(edited.name, { edited = edited.copy(name = it.take(20).trim()) }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("Usuario") })
  CountryField(edited.country) { edited = edited.copy(country = it) }
  OutlinedTextField(
    edited.email, { edited = edited.copy(email = it.trim()) }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("Correo (opcional)") },
    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
  )
  OutlinedTextField(
    password, { password = it }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("Contraseña nueva (opcional)") },
    visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
  )
  SaveButtons(busy, edited.name.length >= 3 && (password.isEmpty() || password.length >= 6), onCancel) { actions.save(edited, password) }
}

@Composable
private fun PhotoButtons(hasPhoto: Boolean, actions: ProfileActions) {
  Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
    TextButton(onClick = actions.changePhoto) { Text(if (hasPhoto) "Cambiar foto" else "Poner foto") }
    if (hasPhoto) TextButton(onClick = actions.removePhoto) { Text("Quitar foto") }
  }
}

@Composable
private fun SaveButtons(busy: Boolean, valid: Boolean, onCancel: () -> Unit, onSave: () -> Unit) {
  Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
    OutlinedButton(onClick = onCancel, Modifier.weight(1f)) { Text("Cancelar") }
    Button(onClick = onSave, Modifier.weight(1f), enabled = valid && !busy) {
      if (busy) CircularProgressIndicator(strokeWidth = 2.dp) else Text("Guardar")
    }
  }
}

@Composable
fun StatsDialog(sections: List<StatsSection>?, error: String?, onClose: () -> Unit) {
  AlertDialog(
    onDismissRequest = onClose, title = { Text("Estadísticas") }, confirmButton = { TextButton(onClick = onClose) { Text("Cerrar") } },
    text = {
      Column(Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        when {
          error != null -> Text(error, color = MaterialTheme.colorScheme.error)
          sections == null -> CircularProgressIndicator()
          sections.isEmpty() -> Text("Aún no hay partidas en línea.")
          else -> sections.forEach { StatsCard(it) }
        }
      }
    },
  )
}

@Composable
private fun LogOutDialog(onConfirm: () -> Unit, onClose: () -> Unit) {
  AlertDialog(
    onDismissRequest = onClose, title = { Text("¿Seguro que quieres cerrar sesión?") },
    confirmButton = { Button(onClick = onConfirm) { Text("Cerrar sesión") } },
    dismissButton = { TextButton(onClick = onClose) { Text("Cancelar") } },
  )
}
