package app.lexico.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class OnlineProfile(val name: String, val country: String)

data class StatsSection(val title: String, val rows: List<Pair<String, String>>)

data class SeekItem(val id: String, val host: String, val country: String, val detail: String, val mine: Boolean)

data class PersonItem(val name: String, val country: String, val busy: Boolean)

data class OnlineHubState(
  val status: String?, val inRoom: Boolean, val seeks: List<SeekItem>, val people: List<PersonItem>, val invite: SeekItem?,
)

class OnlineHubActions(
  val back: () -> Unit, val logOut: () -> Unit, val resume: () -> Unit, val seekClassic: () -> Unit, val createDuplicate: () -> Unit,
  val accept: (String) -> Unit, val challenge: (PersonItem) -> Unit, val decline: (String) -> Unit, val stats: () -> Unit,
)

@Composable
fun OnlineHubScreen(profile: OnlineProfile, state: OnlineHubState, actions: OnlineHubActions) {
  Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
    Header("En línea", actions.back) { BarTextButton("Estadísticas", actions.stats) }
    ProfileCard(profile, state.status, actions.logOut)
    if (state.inRoom) Button(onClick = actions.resume, Modifier.fillMaxWidth().height(52.dp)) { Text("Volver a la partida") }
    PublishButtons(actions)
    SeeksSection(state.seeks, actions.accept)
    PeopleSection(state.people, actions.challenge)
  }
  state.invite?.let { InviteDialog(it, actions) }
}

@Composable
private fun PublishButtons(actions: OnlineHubActions) {
  Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    Button(onClick = actions.seekClassic, Modifier.weight(1f)) { Text("Buscar clásica") }
    Button(onClick = actions.createDuplicate, Modifier.weight(1f)) { Text("Crear duplicada") }
  }
}

@Composable
private fun SeeksSection(seeks: List<SeekItem>, onAccept: (String) -> Unit) {
  SectionTitle("Búsquedas · ${seeks.size}")
  if (seeks.isEmpty()) EmptyCard("Nadie está buscando partida.")
  seeks.forEach { s ->
    PlayerRow(s.host, s.country, s.detail) {
      if (s.mine) Text("Tuya", color = MaterialTheme.colorScheme.onSurfaceVariant) else Button(onClick = { onAccept(s.id) }) { Text("Jugar") }
    }
  }
}

@Composable
private fun PeopleSection(people: List<PersonItem>, onChallenge: (PersonItem) -> Unit) {
  SectionTitle("Conectados · ${people.size}")
  if (people.isEmpty()) EmptyCard("No hay nadie más conectado.")
  people.forEach { p ->
    PlayerRow(p.name, p.country, if (p.busy) "Ocupado" else "Disponible") {
      OutlinedButton(onClick = { onChallenge(p) }, enabled = !p.busy) { Text("Retar") }
    }
  }
}

@Composable
private fun PlayerRow(name: String, country: String, detail: String, action: @Composable () -> Unit) {
  Row(
    Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(14.dp)).padding(horizontal = 10.dp, vertical = 8.dp),
    horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically,
  ) {
    PlayerAvatar(name, highlighted = false)
    Column(Modifier.weight(1f)) {
      Text("$name  ${flagOf(country)}", fontWeight = FontWeight.Bold, maxLines = 1)
      Text(detail, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
    }
    action()
  }
}

@Composable
private fun EmptyCard(text: String) {
  Row(
    Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp)).padding(14.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant)
  }
}

@Composable
private fun InviteDialog(invite: SeekItem, actions: OnlineHubActions) {
  AlertDialog(
    onDismissRequest = {}, title = { Text("${invite.host} ${flagOf(invite.country)} te reta") }, text = { Text(invite.detail) },
    confirmButton = { Button(onClick = { actions.accept(invite.id) }) { Text("Aceptar") } },
    dismissButton = { TextButton(onClick = { actions.decline(invite.id) }) { Text("Rechazar") } },
  )
}

@Composable
private fun ProfileCard(profile: OnlineProfile, status: String?, onLogOut: () -> Unit) {
  val colors = MaterialTheme.colorScheme
  Row(
    Modifier.fillMaxWidth().background(colors.primaryContainer, RoundedCornerShape(16.dp)).padding(12.dp),
    horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically,
  ) {
    PlayerAvatar(profile.name, highlighted = true, size = 48.dp)
    Column(Modifier.weight(1f)) {
      Text("${profile.name}  ${flagOf(profile.country)}", fontWeight = FontWeight.Bold, fontSize = 18.sp)
      Text(status ?: "Conectado", style = MaterialTheme.typography.labelMedium, color = colors.onPrimaryContainer)
    }
    TextButton(onClick = onLogOut) { Text("Salir") }
  }
}

@Composable
fun OnlineStatsScreen(profile: OnlineProfile, sections: List<StatsSection>?, error: String?, onBack: () -> Unit) {
  FitColumn(12.dp) {
    Header("Mis estadísticas", onBack)
    ProfileLine(profile)
    when {
      error != null -> Text(error, color = MaterialTheme.colorScheme.error)
      sections == null -> CircularProgressIndicator()
      sections.isEmpty() -> StatsCard(StatsSection("Aún no hay partidas en línea.", emptyList()))
      else -> sections.forEach { StatsCard(it) }
    }
  }
}

@Composable
private fun ProfileLine(profile: OnlineProfile) {
  Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
    PlayerAvatar(profile.name, highlighted = true)
    Text("${profile.name}  ${flagOf(profile.country)}", fontWeight = FontWeight.Bold)
  }
}

@Composable
private fun StatsCard(section: StatsSection) {
  Column(
    Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp)).padding(14.dp),
    verticalArrangement = Arrangement.spacedBy(6.dp),
  ) {
    Text(section.title, fontWeight = FontWeight.Bold)
    section.rows.forEach { (label, value) ->
      Row(Modifier.fillMaxWidth()) {
        Text(label, Modifier.weight(1f))
        Text(value, fontWeight = FontWeight.Bold)
      }
    }
  }
}

@Composable
fun OnlineWaitScreen(title: String, text: String, failed: Boolean, onBack: () -> Unit) {
  FitColumn(14.dp) {
    Header(title, onBack)
    if (failed) return@FitColumn Text(text, color = MaterialTheme.colorScheme.error)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
      CircularProgressIndicator()
      Text(text)
    }
  }
}

@Composable
fun ServerDialog(current: String, onSave: (String) -> Unit, onClose: () -> Unit) {
  var address by remember { mutableStateOf(current) }
  AlertDialog(
    onDismissRequest = onClose, title = { Text("Servidor") },
    text = { OutlinedTextField(address, { address = it.trim() }, singleLine = true, label = { Text("Dirección") }) },
    confirmButton = { TextButton(onClick = { onSave(address); onClose() }) { Text("Guardar") } },
    dismissButton = { TextButton(onClick = onClose) { Text("Cancelar") } },
  )
}

