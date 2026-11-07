package app.lexico.navigation

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import app.lexico.game.Lexico
import app.lexico.game.modes.duplicate.table.TableState
import app.lexico.game.storage.Mode
import app.lexico.menu.Settings
import app.lexico.menu.ThemeState
import app.lexico.modes.duplicate.seatView
import app.lexico.online.ADJOURNED
import app.lexico.online.AccountForm
import app.lexico.online.LiveGame
import app.lexico.online.Lobby
import app.lexico.online.Online
import app.lexico.online.OnlineClient
import app.lexico.online.OnlinePhotos
import app.lexico.online.OnlineSession
import app.lexico.online.OnlineStatsRow
import app.lexico.online.Person
import app.lexico.online.Proposal
import app.lexico.online.RoomInfo
import app.lexico.online.Seek
import app.lexico.online.shrunkPhoto
import app.lexico.ui.classic.NewOnlineClassicScreen
import app.lexico.ui.common.Disconnected
import app.lexico.ui.common.AccountInput
import app.lexico.ui.common.AdjournedItem
import app.lexico.ui.common.ChatEntry
import app.lexico.ui.common.ContactGroup
import app.lexico.ui.common.ChatContact
import app.lexico.ui.common.Durations
import app.lexico.ui.common.GameItem
import app.lexico.ui.common.GuestLobbyScreen
import app.lexico.ui.common.HostLobbyScreen
import app.lexico.ui.common.LocalChat
import app.lexico.ui.common.LocalPhotos
import app.lexico.ui.common.OnlineAccountScreen
import app.lexico.ui.common.OnlineHubActions
import app.lexico.ui.common.OnlineHubScreen
import app.lexico.ui.common.OnlineHubState
import app.lexico.ui.common.OnlineProfile
import app.lexico.ui.common.OnlineProfileScreen
import app.lexico.ui.common.OnlineWaitScreen
import app.lexico.ui.common.PersonItem
import app.lexico.ui.common.PhotoSource
import app.lexico.ui.common.ProfileActions
import app.lexico.ui.common.ProfileData
import app.lexico.ui.common.SeekItem
import app.lexico.ui.common.StatsDialog
import app.lexico.ui.common.StatsSection
import app.lexico.ui.common.WaitingRoomScreen
import app.lexico.ui.common.flagOf
import app.lexico.ui.duplicate.NewOnlineDuplicateScreen
import kotlinx.coroutines.launch

private const val RIVAL_AWAY = "Tu rival se desconectó. Su reloj sigue corriendo."

private var backupSynced = false

@Composable
internal fun OnlineRoute(mode: Mode?, lexico: Lexico, settings: Settings, nav: Navigator) {
  val online = rememberOnline()
  val session by online.account.session.collectAsState()
  session?.let { OnlineHub(it, mode, online, lexico, settings, nav) } ?: AccountRoute(online, nav)
}

@Composable
internal fun NewOnlineClassicRoute(screen: Screen.OnlineNewClassic, lexico: Lexico, nav: Navigator) {
  val client = rememberClient(lexico)
  val rival = screen.rival
  val title = rival?.let { "Retar a ${it.name} ${flagOf(it.country)}" } ?: "Buscar clásica"
  NewOnlineClassicScreen(title, if (rival == null) "Publicar búsqueda" else "Enviar reto", nav::back) { time, overtime, single, timed ->
    val proposal = Proposal.Classic(time, overtime, single, timed)
    if (rival == null) client.publish(proposal) else client.challenge(rival, proposal)
    nav.back()
  }
}

@Composable
internal fun NewOnlineDuplicateRoute(lexico: Lexico, nav: Navigator) {
  val client = rememberClient(lexico)
  NewOnlineDuplicateScreen(nav::back) { turn, rounds, live ->
    client.publish(Proposal.Duplicate(turn, rounds, live))
    nav.back()
  }
}

@Composable
internal fun OnlineProfileRoute(nav: Navigator) {
  val online = rememberOnline()
  val session = online.account.session.collectAsState().value ?: return
  val scope = rememberCoroutineScope()
  var form by remember { mutableStateOf(ProfileData(session.name, session.country, "")) }
  var notice by remember { mutableStateOf<String?>(null) }
  var busy by remember { mutableStateOf(false) }
  var editing by remember { mutableStateOf(false) }
  var showingStats by remember { mutableStateOf(false) }
  LaunchedEffect(Unit) { online.loadAccount()?.let { form = ProfileData(it.name, it.country, it.email) } }
  val photo = rememberPhotoActions(online) { notice = it }
  val save = { edited: ProfileData, password: String ->
    scope.launch {
      busy = true
      notice = online.editAccount(AccountForm(edited.name, edited.country, password, edited.email))
      if (notice == null) { form = edited; editing = false }
      busy = false
    }
    Unit
  }
  val actions = ProfileActions(nav::back, save, photo.change, photo.remove, { showingStats = true }) { online.logOut(); nav.back() }
  val hasPhoto = OnlinePhotos.key(form.name, form.country) in online.photos.images
  ProvidePhotos(online, Lobby()) { OnlineProfileScreen(form, hasPhoto, busy, notice, editing, { editing = it; notice = null }, actions) }
  if (showingStats) StatsPopup(online) { showingStats = false }
}

@Composable
private fun StatsPopup(online: Online, onClose: () -> Unit) {
  val loaded by produceState<Result<List<OnlineStatsRow>>?>(null) {
    value = online.account.token()?.let { online.api.stats(it) } ?: Result.failure(Exception("La sesión caducó: vuelve a entrar."))
  }
  StatsDialog(loaded?.getOrNull()?.map(::statsSection), loaded?.exceptionOrNull()?.message, onClose)
}

@Composable
internal fun OnlinePlayRoute(lexico: Lexico, nav: Navigator, themes: ThemeState) {
  val client = rememberClient(lexico)
  DisposableEffect(client) { onDispose { client.forgetIfFinished() } }
  val info by client.room.collectAsState()
  val ended by client.ended.collectAsState()
  val status by client.status.collectAsState()
  val current = info
  LaunchedEffect(current, ended) { if (current == null && ended == ADJOURNED) nav.back() }
  when {
    current == null -> OnlineWaitScreen("En línea", ended ?: status ?: "Conectando…", ended != null) { client.forget(); nav.back() }
    current.watching -> Disconnected(status != null && ended == null, nav::home, note = null) {
      ProvidePhotos(rememberOnline(), client.lobby.collectAsState().value) { Watch(client, current, nav, themes) }
    }
    else -> Disconnected(status != null && ended == null, nav::home) { SeatedGame(client, current, lexico, nav, themes) }
  }
}

@Composable
private fun SeatedGame(client: OnlineClient, info: RoomInfo, lexico: Lexico, nav: Navigator, themes: ThemeState) {
  if (info.duplicate) return WithChat(client, info) { DuplicateOnline(client, info, nav, themes) }
  WithChat(client, info) { ProvidePhotos(rememberOnline(), client.lobby.collectAsState().value) { ClassicOnline(client, info, lexico, nav, themes) } }
}

@Composable
internal fun ReturnToOnlineGame(nav: Navigator) {
  val client by rememberOnline().current.collectAsState()
  LaunchedEffect(client) { client?.rejoined?.collect { if (nav.current != Screen.OnlinePlay) nav.go(Screen.OnlinePlay) } }
}

@Composable
private fun AccountRoute(online: Online, nav: Navigator) {
  val scope = rememberCoroutineScope()
  var busy by remember { mutableStateOf(false) }
  var error by remember { mutableStateOf<String?>(null) }
  val submit = { signingUp: Boolean, input: AccountInput ->
    scope.launch {
      busy = true
      error = with(online.account) { if (signingUp) signUp(input.form()) else logIn(input.form()) }
      busy = false
    }
    Unit
  }
  OnlineAccountScreen(busy, error, nav::back, { submit(true, it) }) { submit(false, it) }
}

@Composable
private fun OnlineHub(session: OnlineSession, mode: Mode?, online: Online, lexico: Lexico, settings: Settings, nav: Navigator) {
  val client = rememberClient(lexico)
  LaunchedEffect(session.name) { syncBackupOnce(online, settings) }
  val lobby by client.lobby.collectAsState()
  val room by client.room.collectAsState()
  val game by client.game.collectAsState()
  val status by client.status.collectAsState()
  val refused by client.refused.collectAsState()
  val ended by client.ended.collectAsState()
  JumpWhenSeated(room?.id) { nav.go(Screen.OnlinePlay) }
  val composer = remember { Composer() }
  val inbox by client.inbox.collectAsState()
  val actions = hubActions(client, lobby, nav, composer)
  val seeks = lobby.seeks.filter { mode == null || it.duplicate == (mode == Mode.DUPLICATE) }
  val state = OnlineHubState(
    refused ?: status ?: ended.takeIf { room == null }, room != null && game?.state?.value?.result == null, seeks.map(::seekItem),
    lobby.people.map(::personItem), lobby.invites.firstOrNull()?.let(::seekItem),
    classic = mode != Mode.DUPLICATE, duplicate = mode != Mode.CLASSIC,
    games = lobby.games.filter { mode == null || it.duplicate == (mode == Mode.DUPLICATE) }.map(::gameItem),
    adjourned = lobby.adjourned.filter { mode == null || it.duplicate == (mode == Mode.DUPLICATE) }
      .map { AdjournedItem(it.id, it.opponent, it.country, it.online, it.busy, it.result, it.duplicate) },
    unread = inbox.size,
  )
  ProvidePhotos(online, lobby) {
    OnlineHubScreen(profileOf(session), state, actions)
    if (room == null) MessageDialogs(client, contacts(lobby, emptyList(), emptyList()), null, composer)
  }
}

@Composable
private fun JumpWhenSeated(roomId: String?, jump: () -> Unit) {
  var seen by remember { mutableStateOf(roomId) }
  LaunchedEffect(roomId) {
    if (roomId != null && roomId != seen) jump()
    seen = roomId
  }
}

private fun hubActions(client: OnlineClient, lobby: Lobby, nav: Navigator, composer: Composer): OnlineHubActions {
  val seekOf = { id: String -> (lobby.seeks + lobby.invites).find { it.id == id } }
  return OnlineHubActions(
    back = nav::back, profile = { nav.go(Screen.OnlineProfile) }, resume = { nav.go(Screen.OnlinePlay) },
    seekClassic = { nav.go(Screen.OnlineNewClassic()) }, createDuplicate = { nav.go(Screen.OnlineNewDuplicate) },
    accept = { id -> seekOf(id)?.let { client.accept(it); nav.go(Screen.OnlinePlay) } },
    challenge = { nav.go(Screen.OnlineNewClassic(Person(it.name, it.country, it.busy))) },
    decline = { id -> seekOf(id)?.let(client::decline) },
    cancel = { id -> seekOf(id)?.let(client::cancel) }, start = { id -> seekOf(id)?.let(client::startSeek) },
    watch = { id -> lobby.games.find { it.id == id }?.let { client.watch(it); nav.go(Screen.OnlinePlay) } },
    resumeAdjourned = { id -> lobby.adjourned.find { it.id == id }?.let(client::resume) },
    seenAdjourned = { id -> lobby.adjourned.find { it.id == id }?.let(client::seen) },
    messages = { composer.write() }, write = { p -> composer.write(ChatContact(p.name, p.country, ContactGroup.OTHERS)) },
  )
}

@Composable
private fun ClassicOnline(client: OnlineClient, info: RoomInfo, lexico: Lexico, nav: Navigator, themes: ThemeState) {
  val game by client.game.collectAsState()
  val notice by client.notice.collectAsState()
  val status by client.status.collectAsState()
  val ended by client.ended.collectAsState()
  val rivalOnline by client.rivalOnline.collectAsState()
  val watchers by client.watchers.collectAsState()
  val over = game?.state?.collectAsState()?.value?.result != null
  val shown = status ?: ended ?: RIVAL_AWAY.takeUnless { rivalOnline || over } ?: notice
  var adjourning by remember { mutableStateOf(false) }
  game?.let {
    ClassicPlay(
      it, nav, themes, lexico.finishedGames, shown, handOff = false, pausable = false, closeOnLeave = false,
      watchers = watchers.map(::personItem), onAdjourn = { adjourning = true }, online = true,
    )
    AdjournDialogs(client, info, over || ended != null, adjourning) { adjourning = false }
  }
    ?: WaitingRoomScreen(info.host, ended, { client.leave(); nav.back() }, wifi = false)
}

@Composable
private fun DuplicateOnline(client: OnlineClient, info: RoomInfo, nav: Navigator, themes: ThemeState) {
  val json by client.table.collectAsState()
  val notice by client.notice.collectAsState()
  val status by client.status.collectAsState()
  val ended by client.ended.collectAsState()
  val watchers by client.watchers.collectAsState()
  var adjourning by remember { mutableStateOf(false) }
  var state by remember { mutableStateOf<TableState?>(null) }
  LaunchedEffect(json) { json?.let { state = read(it) } }
  val actions = remember(client) { seatActions(nav, propose = client::propose, ready = client::ready) }
  val table = state
  val leave = { client.leave(); nav.back() }
  when {
    table != null -> {
      ExportableSeat(
        seatView(table, info.me, notice, status ?: ended).copy(watchers = watchers.map(::personItem)), themes.board, actions,
        onAdjourn = { adjourning = true }.takeIf { info.players.size == 2 },
      )
      if (info.players.size == 2) AdjournDialogs(client, info, ended != null, adjourning) { adjourning = false }
    }
    info.first -> HostLobbyScreen(info.me, info.players, ended, client::start, leave, wifi = false)
    else -> GuestLobbyScreen(info.host, info.players, status ?: ended, info.me, leave)
  }
}

@Composable
private fun WithChat(client: OnlineClient, info: RoomInfo, content: @Composable () -> Unit) {
  val composer = remember { Composer() }
  val lobby by client.lobby.collectAsState()
  val watchers by client.watchers.collectAsState()
  val rivals = info.players.filter { it != info.me }
  val list = contacts(lobby, rivals, watchers)
  val fallback = remember(info.id, list.isEmpty()) { list.filter { it.group == ContactGroup.RIVALS }.randomOrNull() }
  val entry = remember { ChatEntry { composer.write() } }
  CompositionLocalProvider(LocalChat provides entry, content = content)
  MessageDialogs(client, list, fallback, composer)
}

private class PhotoActions(val change: () -> Unit, val remove: () -> Unit)

@Composable
private fun rememberPhotoActions(online: Online, onNotice: (String?) -> Unit): PhotoActions {
  val scope = rememberCoroutineScope()
  val context = LocalContext.current
  val save = { base64: String -> scope.launch { onNotice(online.setPhoto(base64)) }; Unit }
  val picker = rememberLauncherForActivityResult(PickVisualMedia()) { uri ->
    uri?.let { scope.launch { shrunkPhoto(context, it)?.let(save) ?: onNotice("No se pudo leer esa foto.") } }
  }
  return PhotoActions({ picker.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly)) }) { save("") }
}

@Composable
private fun ProvidePhotos(online: Online, lobby: Lobby, content: @Composable () -> Unit) {
  val session by online.account.session.collectAsState()
  val source = remember(lobby, session) { LobbyPhotos(online.photos, countriesOf(lobby, session)) }
  CompositionLocalProvider(LocalPhotos provides source, content = content)
}

private class LobbyPhotos(private val photos: OnlinePhotos, private val countries: Map<String, String>) : PhotoSource {
  @Composable
  override fun of(name: String, country: String): ImageBitmap? {
    LaunchedEffect(name, country) { photos.request(name, country) }
    return photos.images[OnlinePhotos.key(name, country)]
  }

  @Composable
  override fun ofSeat(seatName: String): ImageBitmap? {
    val found = SEAT_WITH_COUNTRY.matchEntire(seatName)
    val name = found?.groupValues?.get(1) ?: seatName
    val country = found?.groupValues?.get(2) ?: countries[seatName.lowercase()] ?: return null
    return of(name, country)
  }
}

private val SEAT_WITH_COUNTRY = Regex("^(.*) \\(([A-Z]{2})\\)$")

private fun countriesOf(lobby: Lobby, session: OnlineSession?): Map<String, String> =
  lobby.people.associate { it.name.lowercase() to it.country } + listOfNotNull(session).associate { it.name.lowercase() to it.country }

@Composable
private fun rememberClient(lexico: Lexico): OnlineClient {
  val online = rememberOnline()
  val client = remember { online.client(lexico) }
  DisposableEffect(client) { client.acquire(); onDispose { client.release() } }
  return client
}

@Composable
private fun rememberOnline(): Online {
  val context = LocalContext.current
  return remember { Online.of(context) }
}

private suspend fun syncBackupOnce(online: Online, settings: Settings) {
  if (backupSynced) return
  online.syncBackup(settings.statsSince)?.let { settings.adoptStatsSince(it); backupSynced = true }
}

private fun gameItem(g: LiveGame): GameItem {
  val names = g.players.zip(g.countries) { name, country -> "$name ${flagOf(country)}" }
  return if (g.duplicate) GameItem(g.id, names.joinToString(", "), "Duplicada · ${g.players.size} jugadores", g.watchers)
  else GameItem(g.id, names.joinToString("  vs  "), "Clásica", g.watchers)
}

internal fun personItem(p: Person): PersonItem = PersonItem(p.name, p.country, p.busy)

private fun seekItem(s: Seek): SeekItem =
  SeekItem(s.id, s.host, s.country, seekDetail(s), s.mine, s.duplicate, s.players, s.to, s.resume)

private fun seekDetail(s: Seek): String =
  if (s.resume && s.duplicate) "Partida aplazada · duplicada"
  else if (s.resume) "Partida aplazada · ${classicTime(s)}"
  else if (s.duplicate) "👥 ${s.players} en la sala · ${Durations.format(s.turnMs)} por ronda"
  else if (s.mine && s.to.isNotEmpty()) "Reto a ${s.to} · ${classicTime(s)} · ${if (s.single) "Single" else "Void"}"
  else "Clásica · ${classicTime(s)} · ${if (s.single) "Single" else "Void"}"

private fun classicTime(s: Seek): String =
  if (s.timed) "${Durations.format(s.timeMs)} + ${Durations.format(s.overtimeMs)}" else "Sin tiempo"

private fun profileOf(session: OnlineSession): OnlineProfile = OnlineProfile(session.name, session.country)

private fun AccountInput.form(): AccountForm = AccountForm(name, country, password, email)

private fun statsSection(row: OnlineStatsRow): StatsSection = if (row.mode == "duplicate") duplicateSection(row) else classicSection(row)

private fun classicSection(row: OnlineStatsRow): StatsSection = StatsSection(
  "Clásica",
  listOf(
    "Partidas" to "${row.games}",
    "Victorias" to "${row.wins}  (%.0f %%)".format(100.0 * row.wins / row.games.coerceAtLeast(1)),
    "Derrotas" to "${row.losses}",
    "Empates" to "${row.draws}",
    "Promedio de puntos" to "%.0f".format(row.avgScore),
    "Mayor scrabble" to if (row.bestBingo > 0) "${row.bestBingo}" else "—",
    "Mejor jugada" to "${row.bestPlay}",
  ),
)

private fun duplicateSection(row: OnlineStatsRow): StatsSection = StatsSection(
  "Duplicada",
  listOf(
    "Partidas" to "${row.games}",
    "Primeros puestos" to "${row.wins}",
    "Efectividad promedio" to (row.avgEfficiency?.let { "%.1f %%".format(it * 100) } ?: "—"),
    "Mayor scrabble" to if (row.bestBingo > 0) "${row.bestBingo}" else "—",
    "Mejor jugada" to "${row.bestPlay}",
  ),
)
