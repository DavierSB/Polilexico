package app.lexico.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Badge
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class StatsSection(val title: String, val rows: List<Pair<String, String>>)

data class SeekItem(
  val id: String, val host: String, val country: String, val detail: String, val mine: Boolean,
  val duplicate: Boolean = false, val players: Int = 1, val to: String = "", val resume: Boolean = false,
)

data class AdjournedItem(
  val id: String, val opponent: String, val country: String, val online: Boolean, val busy: Boolean = false, val result: String = "",
  val duplicate: Boolean = false, val untimed: Boolean = false,
) {
  val pending: Boolean get() = result.isEmpty()
  val available: Boolean get() = pending && (online || untimed) && !busy
}

data class GameItem(val id: String, val title: String, val detail: String, val watchers: Int = 0)

data class PersonItem(val name: String, val country: String, val busy: Boolean, val game: String? = null)

data class OnlinePanelState(
  val status: String?, val inRoom: Boolean, val seeks: List<SeekItem>, val classic: Boolean,
  val games: List<GameItem> = emptyList(), val adjourned: List<AdjournedItem> = emptyList(), val people: List<PersonItem> = emptyList(),
)

class OnlinePanelActions(
  val resume: () -> Unit, val seekClassic: () -> Unit, val createDuplicate: () -> Unit, val accept: (String) -> Unit,
  val cancel: (String) -> Unit = {}, val start: (String) -> Unit = {}, val watch: (String) -> Unit = {},
  val resumeAdjourned: (String) -> Unit = {}, val seenAdjourned: (String) -> Unit = {},
  val challenge: (PersonItem) -> Unit = {}, val write: (PersonItem) -> Unit = {},
)

class OnlineTab(val preferred: Boolean, val content: @Composable () -> Unit)

@Composable
fun OnlinePanel(state: OnlinePanelState, actions: OnlinePanelActions, modifier: Modifier = Modifier) {
  Column(modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
    PanelStatus(state, actions)
    Box(Modifier.weight(1f)) { PanelPage { ModeOffers(state, actions) } }
    if (state.games.isNotEmpty()) WatchButton(state.games, actions.watch)
    PublishButton(state, actions)
  }
  PanelNotices(state, actions)
}

@Composable
private fun PanelStatus(state: OnlinePanelState, actions: OnlinePanelActions) {
  state.status?.let { Text(it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
  if (state.inRoom) Button(onClick = actions.resume, Modifier.fillMaxWidth().height(52.dp)) { Text("Volver a la partida") }
}

@Composable
private fun PanelNotices(state: OnlinePanelState, actions: OnlinePanelActions) {
  val verdict = state.adjourned.firstOrNull { !it.pending }
  when {
    verdict != null -> VerdictNotice(verdict.opponent, verdict.country, verdict.result) { actions.seenAdjourned(verdict.id) }
    else -> ResumeOnEntry(state, actions)
  }
}

@Composable
private fun WatchButton(games: List<GameItem>, onWatch: (String) -> Unit) {
  var open by remember { mutableStateOf(false) }
  OutlinedButton(onClick = { open = true }, Modifier.fillMaxWidth()) { ButtonText("Observar partidas (${games.size})") }
  if (open) GamesDialog(games, { open = false; onWatch(it) }) { open = false }
}

@Composable
private fun GamesDialog(games: List<GameItem>, onWatch: (String) -> Unit, close: () -> Unit) {
  AlertDialog(
    onDismissRequest = close, title = { Text("Partidas en juego") },
    text = { Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) { GameList(games, onWatch) } },
    confirmButton = { TextButton(onClick = close) { Text("Cerrar") } },
  )
}

@Composable
private fun PanelPage(content: @Composable ColumnScope.() -> Unit) {
  val scroll = rememberScrollState()
  Column(
    Modifier.fillMaxSize().scrollbar(scroll).verticalScroll(scroll).padding(top = 4.dp, end = 8.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp), content = content,
  )
}

@Composable
private fun ModeOffers(state: OnlinePanelState, actions: OnlinePanelActions) {
  val duplicate = !state.classic
  SeekList(state.seeks.filter { it.duplicate == duplicate }, actions)
  AdjournedList(state.adjourned.filter { it.duplicate == duplicate && it.available }, actions)
  ConnectedList(state.people, PeopleActions(actions.write, actions.challenge.takeIf { state.classic }, actions.watch))
}

class PeopleActions(val write: (PersonItem) -> Unit, val challenge: ((PersonItem) -> Unit)?, val watch: (String) -> Unit)

private const val NOBODY_ONLINE = "No hay nadie en línea."

@Composable
private fun ConnectedList(people: List<PersonItem>, actions: PeopleActions) {
  if (people.isEmpty()) return EmptyState("👥", NOBODY_ONLINE)
  Spacer(Modifier.height(12.dp))
  Text("Conectados", style = MaterialTheme.typography.labelLarge)
  PeopleList(people, actions)
}

@Composable
private fun GameList(games: List<GameItem>, onWatch: (String) -> Unit) {
  if (games.isEmpty()) return EmptyState({ InitialTile("J", highlighted = false, size = 34.dp) }, "No se está jugando ninguna partida.")
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
      if (g.watchers > 0) Text(watchersLabel(g.watchers), style = MaterialTheme.typography.labelLarge)
      Text("Ver", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
    }
  }
}

@Composable
private fun PublishButton(state: OnlinePanelState, actions: OnlinePanelActions) {
  if (state.classic) Button(onClick = actions.seekClassic, Modifier.fillMaxWidth()) { ButtonText("Buscar clásica") }
  else Button(onClick = actions.createDuplicate, Modifier.fillMaxWidth()) { ButtonText("Crear duplicada") }
}

@Composable
private fun SeekList(seeks: List<SeekItem>, actions: OnlinePanelActions) {
  seeks.sortedByDescending { it.mine }.forEach { s ->
    PlayerRow(s.host, s.country, s.detail, mine = s.mine) { SeekActions(s, actions) }
  }
}

@Composable
private fun EmptyState(icon: String, text: String) = EmptyState({ Text(icon, fontSize = 28.sp) }, text)

@Composable
private fun EmptyState(icon: @Composable () -> Unit, text: String) {
  Column(Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
    icon()
    Text(text, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
  }
}

@Composable
private fun SeekActions(s: SeekItem, actions: OnlinePanelActions) {
  if (!s.mine) return Button(onClick = { actions.accept(s.id) }) { Text("Jugar") }
  Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
    if (s.duplicate) Button(onClick = { actions.start(s.id) }, enabled = s.players >= 2) { Text("Empezar") }
    TextButton(onClick = { actions.cancel(s.id) }) { Text("Cancelar") }
  }
}

@Composable
private fun PeopleList(people: List<PersonItem>, actions: PeopleActions) {
  if (people.isEmpty()) EmptyState("👥", NOBODY_ONLINE)
  people.forEach { p ->
    PlayerRow(p.name, p.country, if (p.busy) "Ocupado" else "Disponible") {
      ChatIconButton("Escribir a ${p.name}") { actions.write(p) }
      PersonButton(p, actions)
    }
  }
}

@Composable
private fun PersonButton(p: PersonItem, actions: PeopleActions) {
  if (p.busy) return OutlinedButton(onClick = { p.game?.let(actions.watch) }, enabled = p.game != null) { Text("Ver") }
  actions.challenge?.let { challenge -> OutlinedButton(onClick = { challenge(p) }) { Text("Retar") } }
}

@Composable
fun PeopleDialog(people: List<PersonItem>, actions: PeopleActions, close: () -> Unit) {
  AlertDialog(
    onDismissRequest = close, title = { Text("Conectados") },
    text = { Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) { PeopleList(people, actions) } },
    confirmButton = { TextButton(onClick = close) { Text("Cerrar") } },
  )
}

@Composable
fun MessagesButton(unread: Int, onClick: () -> Unit) {
  BadgedBox(badge = { if (unread > 0) Badge { Text("$unread") } }) { ChatIconButton("Mensajes", onClick) }
}

@Composable
internal fun PlayerRow(
  name: String, country: String, detail: String? = null, mine: Boolean = false, onClick: (() -> Unit)? = null, action: @Composable () -> Unit,
) {
  val colors = MaterialTheme.colorScheme
  val shape = RoundedCornerShape(14.dp)
  Row(
    Modifier.fillMaxWidth().clip(shape).then(onClick?.let { Modifier.clickable(onClick = it) } ?: Modifier)
      .background(if (mine) colors.primaryContainer else colors.surfaceVariant, shape)
      .padding(horizontal = 10.dp, vertical = 8.dp),
    horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically,
  ) {
    PersonAvatar(name, country)
    Column(Modifier.weight(1f)) {
      Text("$name  ${flagOf(country)}", fontWeight = FontWeight.Bold, maxLines = 1)
      detail?.let { Text(it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1) }
    }
    action()
  }
}

internal fun inviteTitle(invite: SeekItem): String =
  if (invite.resume) "${invite.host} ${flagOf(invite.country)} quiere retomar la partida aplazada" else "${invite.host} ${flagOf(invite.country)} te reta"

@Composable
private fun ResumeOnEntry(state: OnlinePanelState, actions: OnlinePanelActions) {
  var open by remember { mutableStateOf<List<AdjournedItem>>(emptyList()) }
  val online = state.adjourned.filter { it.available && it.online }
  LaunchedEffect(online.map { it.id }, state.inRoom) {
    val due = ResumeReminders.due(online)
    if (due.isNotEmpty() && !state.inRoom && open.isEmpty()) { ResumeReminders.mark(due); open = due }
  }
  val shown = open.filter { game -> online.any { it.id == game.id } }
  if (shown.isEmpty()) return
  val close = { open = emptyList() }
  if (shown.size == 1) ResumeOneDialog(shown[0], { close(); actions.resumeAdjourned(shown[0].id) }, close)
  else ResumeManyDialog(shown, close)
}

private object ResumeReminders {
  private val shownAt = mutableMapOf<String, Long>()

  fun due(games: List<AdjournedItem>): List<AdjournedItem> =
    games.filter { System.currentTimeMillis() - (shownAt[it.id] ?: 0L) > REMIND_EVERY_MS }

  fun mark(games: List<AdjournedItem>) = games.forEach { shownAt[it.id] = System.currentTimeMillis() }
}

private const val REMIND_EVERY_MS = 30 * 60_000L

@Composable
private fun ResumeOneDialog(game: AdjournedItem, onResume: () -> Unit, close: () -> Unit) {
  AlertDialog(
    onDismissRequest = close, icon = { PersonAvatar(game.opponent, game.country, size = 56.dp) },
    title = { Text("${game.opponent} ${flagOf(game.country)}") },
    text = { Text("${game.opponent}, con quien tienes una partida pendiente, está en línea y puedes retomarla.") },
    confirmButton = { Button(onClick = onResume) { Text("Retomar") } },
    dismissButton = { TextButton(onClick = close) { Text("Ahora no") } },
  )
}

@Composable
private fun ResumeManyDialog(games: List<AdjournedItem>, close: () -> Unit) {
  AlertDialog(
    onDismissRequest = close,
    icon = { Row(horizontalArrangement = Arrangement.spacedBy((-10).dp)) { games.take(4).forEach { PersonAvatar(it.opponent, it.country, size = 44.dp) } } },
    text = { Text("Varios de los jugadores con quienes tienes partidas pendientes están en línea y puedes retomar sus partidas.") },
    confirmButton = { TextButton(onClick = close) { Text("Entendido") } },
  )
}

@Composable
private fun AdjournedList(games: List<AdjournedItem>, actions: OnlinePanelActions) {
  val pending = games.filter { it.pending }
  if (pending.isEmpty()) return
  Spacer(Modifier.height(12.dp))
  Text("Partidas aplazadas", style = MaterialTheme.typography.labelLarge)
  pending.forEach { g ->
    PlayerRow(g.opponent, g.country, adjournedDetail(g)) {
      Button(onClick = { actions.resumeAdjourned(g.id) }, enabled = g.available) { Text("Retomar") }
    }
  }
}

private fun adjournedDetail(g: AdjournedItem): String = when {
  !g.online -> "Desconectado · aplazada"
  g.busy -> "Jugando otra partida · aplazada"
  else -> "En línea · aplazada"
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
private fun ClosedScreen(title: String, text: String, onBack: () -> Unit) {
  Column(Modifier.fillMaxSize()) {
    Header(title, onBack)
    Column(
      Modifier.fillMaxWidth().weight(1f).padding(horizontal = 24.dp),
      verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically), horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Text(
        text.removeSuffix("."), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center,
      )
      Button(onClick = onBack) { Text("Volver al lobby") }
    }
  }
}

@Composable
fun OnlineWaitScreen(title: String, text: String, failed: Boolean, onBack: () -> Unit) {
  if (failed) return ClosedScreen(title, text, onBack)
  FitColumn(14.dp) {
    Header(title, onBack)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
      CircularProgressIndicator()
      Text(text)
    }
  }
}
