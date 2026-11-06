package app.lexico.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

data class OnlineProfile(val name: String, val country: String)

data class StatsSection(val title: String, val rows: List<Pair<String, String>>)

data class SeekItem(
  val id: String, val host: String, val country: String, val detail: String, val mine: Boolean,
  val duplicate: Boolean = false, val players: Int = 1, val to: String = "", val resume: Boolean = false,
)

data class AdjournedItem(
  val id: String, val opponent: String, val country: String, val online: Boolean, val busy: Boolean = false, val result: String = "",
  val duplicate: Boolean = false,
) {
  val pending: Boolean get() = result.isEmpty()
  val available: Boolean get() = pending && online && !busy
}

data class GameItem(val id: String, val title: String, val detail: String, val watchers: Int = 0)

data class PersonItem(val name: String, val country: String, val busy: Boolean)

data class OnlineHubState(
  val status: String?, val inRoom: Boolean, val seeks: List<SeekItem>, val people: List<PersonItem>, val invite: SeekItem?,
  val classic: Boolean = true, val duplicate: Boolean = true, val games: List<GameItem> = emptyList(),
  val adjourned: List<AdjournedItem> = emptyList(),
)

class OnlineHubActions(
  val back: () -> Unit, val profile: () -> Unit, val resume: () -> Unit, val seekClassic: () -> Unit, val createDuplicate: () -> Unit,
  val accept: (String) -> Unit, val challenge: (PersonItem) -> Unit, val decline: (String) -> Unit,
  val cancel: (String) -> Unit = {}, val start: (String) -> Unit = {}, val watch: (String) -> Unit = {},
  val resumeAdjourned: (String) -> Unit = {}, val seenAdjourned: (String) -> Unit = {},
)

@Composable
fun OnlineHubScreen(profile: OnlineProfile, state: OnlineHubState, actions: OnlineHubActions) {
  Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
    CompositionLocalProvider(LocalHomeButton provides null) {
      Header("En línea", actions.back) { HubProfile(profile, actions.profile) }
    }
    state.status?.let { Text(it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    if (state.inRoom) Button(onClick = actions.resume, Modifier.fillMaxWidth().height(52.dp)) { Text("Volver a la partida") }
    val pager = rememberPagerState(if (state.duplicate && !state.classic) HubTab.DUPLICATES.ordinal else 0) { HubTab.entries.size }
    HubTabs(pager, state)
    HorizontalPager(pager, Modifier.weight(1f), verticalAlignment = Alignment.Top) { page -> TabPage { TabContent(HubTab.entries[page], state, actions) } }
    PublishButtons(state, actions)
  }
  val verdict = state.adjourned.firstOrNull { !it.pending }
  when {
    state.invite != null -> InviteDialog(state.invite, actions)
    verdict != null -> VerdictNotice(verdict.opponent, verdict.country, verdict.result) { actions.seenAdjourned(verdict.id) }
    else -> ResumeOnEntry(state, actions)
  }
}

private enum class HubTab(val label: String) { CLASSICS("Clásicas"), DUPLICATES("Duplicadas"), GAMES("En juego"), PEOPLE("Conectados") }

private fun HubTab.count(state: OnlineHubState): Int = when (this) {
  HubTab.CLASSICS -> state.seeks.count { !it.duplicate }
  HubTab.DUPLICATES -> state.seeks.count { it.duplicate }
  HubTab.PEOPLE -> state.people.size
  HubTab.GAMES -> state.games.size
}

@Composable
private fun HubTabs(pager: PagerState, state: OnlineHubState) {
  val scope = rememberCoroutineScope()
  Row(
    Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp)).padding(3.dp),
    horizontalArrangement = Arrangement.spacedBy(3.dp),
  ) {
    HubTab.entries.forEach { tab ->
      TabCell(tab.label, tab.count(state), pager.currentPage == tab.ordinal, Modifier.weight(1f)) {
        scope.launch { pager.animateScrollToPage(tab.ordinal) }
      }
    }
  }
}

@Composable
private fun TabCell(label: String, count: Int, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
  val colors = MaterialTheme.colorScheme
  val ink = if (selected) colors.onPrimary else colors.onSurfaceVariant
  Column(
    modifier.clip(RoundedCornerShape(11.dp)).background(if (selected) colors.primary else colors.surfaceVariant)
      .clickable(onClick = onClick).padding(horizontal = 2.dp, vertical = 6.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    BasicText(
      label, maxLines = 1, autoSize = TextAutoSize.StepBased(minFontSize = 10.sp, maxFontSize = 14.sp),
      style = TextStyle(color = ink, fontWeight = FontWeight.Bold),
    )
    Text("$count", color = ink, style = MaterialTheme.typography.labelMedium)
  }
}

@Composable
private fun TabPage(content: @Composable ColumnScope.() -> Unit) {
  val scroll = rememberScrollState()
  Column(
    Modifier.fillMaxSize().scrollbar(scroll).verticalScroll(scroll).padding(top = 4.dp, end = 8.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp), content = content,
  )
}

@Composable
private fun TabContent(tab: HubTab, state: OnlineHubState, actions: OnlineHubActions) = when (tab) {
  HubTab.CLASSICS -> {
    SeekList(state.seeks.filter { !it.duplicate }, "No hay clásicas ofrecidas.", actions)
    AdjournedList(state.adjourned.filter { !it.duplicate }, actions)
  }
  HubTab.DUPLICATES -> {
    SeekList(state.seeks.filter { it.duplicate }, "No hay duplicadas ofrecidas.", actions)
    AdjournedList(state.adjourned.filter { it.duplicate }, actions)
  }
  HubTab.PEOPLE -> PeopleList(state.people, actions.challenge)
  HubTab.GAMES -> GameList(state.games, actions.watch)
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
private fun PublishButtons(state: OnlineHubState, actions: OnlineHubActions) {
  Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    if (state.classic) Button(onClick = actions.seekClassic, Modifier.weight(1f)) { Text("Buscar clásica", maxLines = 1) }
    if (state.duplicate) Button(onClick = actions.createDuplicate, Modifier.weight(1f)) { Text("Crear duplicada", maxLines = 1) }
  }
}

@Composable
private fun SeekList(seeks: List<SeekItem>, empty: String, actions: OnlineHubActions) {
  if (seeks.isEmpty()) return EmptyState("🔍", empty)
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
private fun SeekActions(s: SeekItem, actions: OnlineHubActions) {
  if (!s.mine) return Button(onClick = { actions.accept(s.id) }) { Text("Jugar") }
  Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
    if (s.duplicate) Button(onClick = { actions.start(s.id) }, enabled = s.players >= 2) { Text("Empezar") }
    TextButton(onClick = { actions.cancel(s.id) }) { Text("Cancelar") }
  }
}

@Composable
private fun PeopleList(people: List<PersonItem>, onChallenge: (PersonItem) -> Unit) {
  if (people.isEmpty()) EmptyState("👥", "No hay nadie más conectado.")
  people.forEach { p ->
    PlayerRow(p.name, p.country, if (p.busy) "Ocupado" else "Disponible") {
      OutlinedButton(onClick = { onChallenge(p) }, enabled = !p.busy) { Text("Retar") }
    }
  }
}

@Composable
internal fun PlayerRow(name: String, country: String, detail: String? = null, mine: Boolean = false, action: @Composable () -> Unit) {
  val colors = MaterialTheme.colorScheme
  Row(
    Modifier.fillMaxWidth().background(if (mine) colors.primaryContainer else colors.surfaceVariant, RoundedCornerShape(14.dp))
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

private fun inviteTitle(invite: SeekItem): String =
  if (invite.resume) "${invite.host} ${flagOf(invite.country)} quiere retomar la partida aplazada" else "${invite.host} ${flagOf(invite.country)} te reta"

@Composable
private fun ResumeOnEntry(state: OnlineHubState, actions: OnlineHubActions) {
  var asked by remember { mutableStateOf(false) }
  var open by remember { mutableStateOf(false) }
  val online = state.adjourned.filter { it.available }
  LaunchedEffect(online.isNotEmpty(), state.inRoom) {
    if (online.isNotEmpty() && !state.inRoom && !asked) { asked = true; open = true }
  }
  if (!open || online.isEmpty()) return
  val close = { open = false }
  if (online.size == 1) ResumeOneDialog(online[0], { close(); actions.resumeAdjourned(online[0].id) }, close)
  else ResumeManyDialog(online, close)
}

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
private fun AdjournedList(games: List<AdjournedItem>, actions: OnlineHubActions) {
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
private fun InviteDialog(invite: SeekItem, actions: OnlineHubActions) {
  AlertDialog(
    onDismissRequest = {}, icon = { PersonAvatar(invite.host, invite.country, size = 56.dp) },
    title = { Text(inviteTitle(invite)) }, text = { Text(invite.detail) },
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
