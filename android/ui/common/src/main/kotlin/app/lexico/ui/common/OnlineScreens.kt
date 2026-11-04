package app.lexico.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class OnlineProfile(val name: String, val country: String)

data class StatsSection(val title: String, val rows: List<Pair<String, String>>)

data class SeekItem(
  val id: String, val host: String, val country: String, val detail: String, val mine: Boolean,
  val duplicate: Boolean = false, val players: Int = 1, val to: String = "",
)

data class GameItem(val id: String, val title: String, val detail: String)

data class PersonItem(val name: String, val country: String, val busy: Boolean)

data class OnlineHubState(
  val status: String?, val inRoom: Boolean, val seeks: List<SeekItem>, val people: List<PersonItem>, val invite: SeekItem?,
  val classic: Boolean = true, val duplicate: Boolean = true, val games: List<GameItem> = emptyList(),
)

class OnlineHubActions(
  val back: () -> Unit, val profile: () -> Unit, val resume: () -> Unit, val seekClassic: () -> Unit, val createDuplicate: () -> Unit,
  val accept: (String) -> Unit, val challenge: (PersonItem) -> Unit, val decline: (String) -> Unit,
  val cancel: (String) -> Unit = {}, val start: (String) -> Unit = {}, val watch: (String) -> Unit = {},
)

@Composable
fun OnlineHubScreen(profile: OnlineProfile, state: OnlineHubState, actions: OnlineHubActions) {
  Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
    CompositionLocalProvider(LocalHomeButton provides null) {
      Header("En línea", actions.back) { HubProfile(profile, actions.profile) }
    }
    state.status?.let { Text(it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    val scroll = rememberScrollState()
    Column(Modifier.weight(1f).scrollbar(scroll).verticalScroll(scroll).padding(end = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
      if (state.inRoom) Button(onClick = actions.resume, Modifier.fillMaxWidth().height(52.dp)) { Text("Volver a la partida") }
      GamesSection(state.games, actions.watch)
      SeeksSection(state.seeks, actions)
      PeopleSection(state.people, actions.challenge)
    }
    PublishButtons(state, actions)
  }
  state.invite?.let { InviteDialog(it, actions) }
}

@Composable
private fun GamesSection(games: List<GameItem>, onWatch: (String) -> Unit) {
  if (games.isEmpty()) return
  SectionTitle("En juego · ${games.size}")
  games.forEach { g ->
    Row(
      Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(14.dp)).clickable { onWatch(g.id) }
        .padding(horizontal = 12.dp, vertical = 10.dp),
      horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(Modifier.weight(1f)) {
        Text(g.title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(g.detail, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
      }
      Text("Ver", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
    }
  }
}

@Composable
private fun PublishButtons(state: OnlineHubState, actions: OnlineHubActions) {
  Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    if (state.classic) Button(onClick = actions.seekClassic, Modifier.weight(1f)) { Text("Buscar clásica", maxLines = 1) }
    if (state.duplicate) Button(onClick = actions.createDuplicate, Modifier.weight(1f)) { Text("Crear duplicada", maxLines = 1) }
  }
}

@Composable
private fun SeeksSection(seeks: List<SeekItem>, actions: OnlineHubActions) {
  if (seeks.isEmpty()) return EmptyState("🔍", "No se está buscando ninguna partida.")
  SeekGroup("Clásicas", seeks.filter { !it.duplicate }, actions)
  SeekGroup("Duplicadas", seeks.filter { it.duplicate }, actions)
}

@Composable
private fun SeekGroup(title: String, seeks: List<SeekItem>, actions: OnlineHubActions) {
  if (seeks.isEmpty()) return
  SectionTitle("$title · ${seeks.size}")
  seeks.sortedByDescending { it.mine }.forEach { s ->
    PlayerRow(s.host, s.country, s.detail, mine = s.mine) { SeekActions(s, actions) }
  }
}

@Composable
private fun EmptyState(icon: String, text: String) {
  Column(Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
    Text(icon, fontSize = 28.sp)
    Text(text, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
  }
}

@Composable
private fun SeekActions(s: SeekItem, actions: OnlineHubActions) {
  if (!s.mine) return Button(onClick = { actions.accept(s.id) }) { Text("Jugar") }
  Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
    if (s.duplicate) Button(onClick = { actions.start(s.id) }, enabled = s.players >= 2) { Text("Empezar") }
    TextButton(onClick = { actions.cancel(s.id) }) { Text("Cancelar") }
  }
}

@Composable
private fun PeopleSection(people: List<PersonItem>, onChallenge: (PersonItem) -> Unit) {
  SectionTitle("Conectados · ${people.size}")
  if (people.isEmpty()) EmptyState("👥", "No hay nadie más conectado.")
  people.forEach { p ->
    PlayerRow(p.name, p.country, if (p.busy) "Ocupado" else "Disponible") {
      OutlinedButton(onClick = { onChallenge(p) }, enabled = !p.busy) { Text("Retar") }
    }
  }
}

@Composable
private fun PlayerRow(name: String, country: String, detail: String, mine: Boolean = false, action: @Composable () -> Unit) {
  val colors = MaterialTheme.colorScheme
  Row(
    Modifier.fillMaxWidth().background(if (mine) colors.primaryContainer else colors.surfaceVariant, RoundedCornerShape(14.dp))
      .padding(horizontal = 10.dp, vertical = 8.dp),
    horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically,
  ) {
    PersonAvatar(name, country)
    Column(Modifier.weight(1f)) {
      Text("$name  ${flagOf(country)}", fontWeight = FontWeight.Bold, maxLines = 1)
      Text(detail, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
    }
    action()
  }
}

@Composable
private fun InviteDialog(invite: SeekItem, actions: OnlineHubActions) {
  AlertDialog(
    onDismissRequest = {}, icon = { PersonAvatar(invite.host, invite.country, size = 56.dp) },
    title = { Text("${invite.host} ${flagOf(invite.country)} te reta") }, text = { Text(invite.detail) },
    confirmButton = { Button(onClick = { actions.accept(invite.id) }) { Text("Aceptar") } },
    dismissButton = { TextButton(onClick = { actions.decline(invite.id) }) { Text("Rechazar") } },
  )
}

@Composable
private fun HubProfile(profile: OnlineProfile, onOpen: () -> Unit) {
  Row(Modifier.widthIn(max = 170.dp).clip(RoundedCornerShape(10.dp)).clickable(onClick = onOpen).padding(horizontal = 6.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
    PersonAvatar(profile.name, profile.country, highlighted = true, size = 26.dp)
    Text("${profile.name} ${flagOf(profile.country)}", style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
  }
}

@Composable
internal fun StatsCard(section: StatsSection) {
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

