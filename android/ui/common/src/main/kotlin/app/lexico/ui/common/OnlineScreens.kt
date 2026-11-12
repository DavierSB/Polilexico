package app.lexico.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties

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

data class GameItem(val id: String, val title: String, val detail: String, val watchers: Int = 0, val sides: Pair<GameSide, GameSide>? = null, val score: String = "", val duplicate: DuplicateLine? = null)

data class GameSide(val name: String, val country: String)

data class PersonItem(val name: String, val country: String, val busy: Boolean, val game: String? = null, val adjourned: String? = null)

data class OnlinePanelState(
  val status: String?, val inRoom: Boolean, val seeks: List<SeekItem>, val classic: Boolean,
  val games: List<GameItem> = emptyList(), val adjourned: List<AdjournedItem> = emptyList(), val people: List<PersonItem> = emptyList(),
)

class OnlinePanelActions(
  val resume: () -> Unit, val seekClassic: () -> Unit, val createDuplicate: () -> Unit, val accept: (String) -> Unit,
  val cancel: (String) -> Unit = {}, val start: (String) -> Unit = {}, val watch: (String) -> Unit = {},
  val resumeAdjourned: (String) -> Unit = {}, val seenAdjourned: (String) -> Unit = {},
  val challenge: (PersonItem) -> Unit = {}, val write: (PersonItem) -> Unit = {}, val finished: () -> Unit = {},
)

class OnlineTab(
  val preferred: Boolean, val label: @Composable () -> String = { "En línea" }, val onChosen: (Boolean) -> Unit = {}, val content: @Composable () -> Unit,
)

fun onlineLabel(count: Int?): String = count?.let { "En línea ($it)" } ?: "En línea"

@Composable
fun OnlinePanel(state: OnlinePanelState, actions: OnlinePanelActions, modifier: Modifier = Modifier) {
  Column(modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
    PanelStatus(state, actions)
    Box(Modifier.weight(1f)) { PanelBody(state, actions) }
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
private fun PanelBody(state: OnlinePanelState, actions: OnlinePanelActions) {
  val available = if (state.classic) state.people.filter { !it.busy } else emptyList()
  if (available.isEmpty() && state.games.isEmpty()) return TopPage(Modifier.fillMaxSize()) { ModeOffers(state, actions) }
  TopThenFill(
    lowerReserve(state.games.isNotEmpty(), available.isNotEmpty()), { TopPage(Modifier.fillMaxWidth()) { ModeOffers(state, actions) } },
    { LowerSections(state.games, available, PeopleActions(actions.write, actions.challenge, actions.watch, actions.resumeAdjourned)) },
  )
}

@Composable
private fun TopPage(modifier: Modifier, content: @Composable ColumnScope.() -> Unit) {
  val scroll = rememberScrollState()
  Column(modifier.scrollbar(scroll).verticalScroll(scroll).padding(top = 4.dp, end = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
}

@Composable
private fun ModeOffers(state: OnlinePanelState, actions: OnlinePanelActions) {
  val duplicate = !state.classic
  val (sent, seeks) = state.seeks.filter { it.duplicate == duplicate }.partition(::isSentChallenge)
  val adjourned = state.adjourned.filter { it.duplicate == duplicate && it.available }
  SentChallenges(sent, actions)
  if (seeks.isNotEmpty()) PanelSection("Ofertas de partidas", first = sent.isEmpty())
  SeekList(seeks, actions)
  AdjournedList(adjourned, actions)
  if (sent.isNotEmpty() || seeks.isNotEmpty() || adjourned.any { it.pending } || state.games.isNotEmpty()) return
  if (duplicate) NoDuplicates(state.people.count { !it.busy }) else NoClassics()
}

private fun isSentChallenge(s: SeekItem): Boolean = s.mine && s.to.isNotEmpty()

@Composable
private fun SentChallenges(sent: List<SeekItem>, actions: OnlinePanelActions) {
  if (sent.isEmpty()) return
  PanelSection("Retos enviados", first = true)
  SeekList(sent, actions)
}

@Composable
internal fun PanelSection(text: String, first: Boolean = false) {
  if (!first) Spacer(Modifier.height(8.dp))
  Text(text, style = MaterialTheme.typography.labelLarge)
}

@Composable
private fun NoClassics() = EmptyState({ InitialTile("C", highlighted = false, size = 34.dp) }, "No hay ofertas ni partidas en curso.")

@Composable
internal fun MoreButton(hidden: Int, onClick: () -> Unit) {
  TextButton(onClick = onClick, Modifier.fillMaxWidth().height(36.dp)) { Text("$hidden más") }
}

@Composable
internal fun GamesDialog(games: List<GameItem>, onWatch: (String) -> Unit, close: () -> Unit) {
  val scroll = rememberScrollState()
  AlertDialog(
    onDismissRequest = close, title = { Text("Partidas en curso") },
    modifier = Modifier.padding(horizontal = 16.dp), properties = DialogProperties(usePlatformDefaultWidth = false),
    text = {
      Column(Modifier.scrollbar(scroll).verticalScroll(scroll).padding(end = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        games.forEach { CompactGameRow(it) { onWatch(it.id) } }
      }
    },
    confirmButton = { TextButton(onClick = close) { Text("Cerrar") } },
  )
}

@Composable
internal fun CompactGameRow(g: GameItem, onClick: () -> Unit) {
  Row(
    Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.surfaceVariant).clickable(onClick = onClick)
      .padding(horizontal = 10.dp, vertical = 6.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically,
  ) {
    g.duplicate?.let { return@Row DuplicateRowContent(it) }
    val sides = g.sides ?: return@Row PlainSides(g.title, g.detail)
    SideCell(sides.first, Alignment.CenterStart)
    if (g.score.isNotEmpty()) Text(g.score, fontWeight = FontWeight.Bold)
    SideCell(sides.second, Alignment.CenterEnd)
  }
}

@Composable
private fun RowScope.SideCell(side: GameSide, alignment: Alignment) {
  Box(Modifier.weight(1f), contentAlignment = alignment) { NameFlag(side.name, side.country, weight = null) }
}

@Composable
private fun RowScope.PlainSides(first: String, second: String) {
  Text(first, Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
  Text(second, Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.End)
}

@Composable
private fun NoDuplicates(idle: Int) {
  Column(Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
    InitialTile("D", highlighted = false, size = 34.dp)
    Text(noDuplicatesText(idle), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
  }
}

private fun noDuplicatesText(idle: Int): AnnotatedString = buildAnnotatedString {
  if (idle == 0) return@buildAnnotatedString append("No hay duplicadas publicadas.")
  append(if (idle == 1) "Si creas una duplicada, la recibirá " else "Si creas una duplicada, la recibirán ")
  withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(if (idle == 1) "1 jugador" else "$idle jugadores") }
  append(" en línea.")
}

class PeopleActions(
  val write: (PersonItem) -> Unit, val challenge: ((PersonItem) -> Unit)?, val watch: (String) -> Unit, val resume: (String) -> Unit = {},
)

private const val NOBODY_ONLINE = "No hay nadie en línea."

@Composable
private fun PublishButton(state: OnlinePanelState, actions: OnlinePanelActions) {
  if (!state.classic) return Button(onClick = actions.createDuplicate, Modifier.fillMaxWidth()) { ButtonText("Crear duplicada") }
  Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    Button(onClick = actions.seekClassic, Modifier.weight(1f)) { ButtonText("Buscar clásica") }
    OutlinedButton(onClick = actions.finished, Modifier.weight(1f)) { ButtonText("Terminadas") }
  }
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
internal fun PeopleList(people: List<PersonItem>, actions: PeopleActions) {
  if (people.isEmpty()) EmptyState("👥", NOBODY_ONLINE)
  people.forEach { p ->
    PlayerRow(p.name, p.country) {
      ChatIconButton("Escribir a ${p.name}") { actions.write(p) }
      PersonButton(p, actions)
    }
  }
}

@Composable
private fun PersonButton(p: PersonItem, actions: PeopleActions) {
  if (p.busy) return CompactButton("Ver", enabled = p.game != null) { p.game?.let(actions.watch) }
  p.adjourned?.let { id -> return Button(onClick = { actions.resume(id) }, contentPadding = PaddingValues(horizontal = 12.dp)) { Text("Retomar", maxLines = 1) } }
  actions.challenge?.let { challenge -> Button(onClick = { challenge(p) }, contentPadding = PaddingValues(horizontal = 12.dp)) { Text("Retar", maxLines = 1) } }
}

@Composable
private fun CompactButton(text: String, enabled: Boolean = true, onClick: () -> Unit) {
  OutlinedButton(onClick, enabled = enabled, contentPadding = PaddingValues(horizontal = 12.dp)) { Text(text, maxLines = 1) }
}

@Composable
fun PeopleDialog(people: List<PersonItem>, actions: PeopleActions, title: String = "Conectados", close: () -> Unit) {
  AlertDialog(
    onDismissRequest = close, title = { Text(title) },
    modifier = Modifier.padding(horizontal = 16.dp), properties = DialogProperties(usePlatformDefaultWidth = false),
    text = { ScrollingPeople(people, actions) },
    confirmButton = { TextButton(onClick = close) { Text("Cerrar") } },
  )
}

@Composable
private fun ScrollingPeople(people: List<PersonItem>, actions: PeopleActions) {
  val scroll = rememberScrollState()
  Column(Modifier.scrollbar(scroll).verticalScroll(scroll).padding(end = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { PeopleList(people, actions) }
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
    horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically,
  ) {
    PlayerIdentity(name, country, detail, profile = onClick == null)
    action()
  }
}

@Composable
private fun RowScope.PlayerIdentity(name: String, country: String, detail: String?, profile: Boolean) {
  Row(Modifier.weight(1f).then(if (profile) Modifier.opensProfile(name, country) else Modifier), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
    PersonAvatar(name, country)
    Column {
      NameFlag(name, country)
      detail?.let { Text(it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1) }
    }
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

fun snoozeResumeReminder(roomCode: String) = ResumeReminders.snooze(roomCode)

private object ResumeReminders {
  private val shownAt = mutableMapOf<String, Long>()
  private val snoozedRooms = mutableMapOf<String, Long>()

  fun snooze(roomCode: String) {
    snoozedRooms[roomCode] = System.currentTimeMillis()
  }

  fun due(games: List<AdjournedItem>): List<AdjournedItem> {
    games.forEach { g -> snoozedRooms[g.id.substringBefore('-')]?.let { shownAt.putIfAbsent(g.id, it) } }
    return games.filter { System.currentTimeMillis() - (shownAt[it.id] ?: 0L) > REMIND_EVERY_MS }
  }

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
  PanelSection("Partidas aplazadas")
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
