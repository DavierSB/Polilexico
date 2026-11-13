package app.lexico.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.ui.graphics.asImageBitmap
import android.util.Base64
import android.graphics.BitmapFactory
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
import app.lexico.games.onlineRows
import app.lexico.online.ADJOURNED
import app.lexico.online.AdjournedGame
import app.lexico.online.GAME_GONE
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
import app.lexico.ui.games.GameFolder
import app.lexico.ui.classic.NewOnlineClassicScreen
import app.lexico.ui.common.AwaitingReconnection
import app.lexico.ui.common.Disconnected
import app.lexico.ui.common.GoneCover
import app.lexico.ui.common.DuplicateLine
import app.lexico.ui.common.AdjournedCover
import app.lexico.ui.common.CLOCK_RUNS
import app.lexico.ui.common.AccountInput
import app.lexico.ui.common.AdjournedItem
import app.lexico.ui.common.ChatEntry
import app.lexico.ui.common.ChatContact
import app.lexico.ui.common.ContactGroup
import app.lexico.ui.common.Durations
import app.lexico.ui.common.GameItem
import app.lexico.ui.common.GameSide
import app.lexico.ui.common.GuestLobbyScreen
import app.lexico.ui.common.HostLobbyScreen
import app.lexico.ui.common.LocalChat
import app.lexico.ui.common.LocalHomeButton
import app.lexico.ui.common.LobbyExit
import app.lexico.ui.common.LocalPhotos
import app.lexico.ui.common.OnlineAccountScreen
import app.lexico.ui.common.OnlinePanel
import app.lexico.ui.common.OnlinePanelActions
import app.lexico.ui.common.OnlinePanelState
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
import app.lexico.ui.common.ROOM_CONTACT
import app.lexico.ui.duplicate.NewOnlineDuplicateScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

private const val RIVAL_AWAY = "Tu rival se desconectó."
private const val RIVAL_CLOCK = " Su reloj sigue corriendo."

private var backupSynced = false

@Composable
internal fun OnlineAccountRoute(nav: Navigator) {
  val online = rememberOnline()
  val session by online.account.session.collectAsState()
  LaunchedEffect(session != null) { if (session != null) nav.back() }
  if (session == null) AccountRoute(online, nav)
}

@Composable
internal fun NewOnlineClassicRoute(screen: Screen.OnlineNewClassic, lexico: Lexico, nav: Navigator) {
  val client = rememberClient(lexico)
  val rival = screen.rival
  val title = rival?.let { "Retar a ${it.name} ${flagOf(it.country)}" } ?: "Buscar clásica"
  InvolvedGate(client, nav) {
    NewOnlineClassicScreen(title, if (rival == null) "Publicar búsqueda" else "Enviar reto", nav::back) { time, overtime, single, timed ->
      val proposal = Proposal.Classic(time, overtime, single, timed)
      if (rival == null) client.publish(proposal) else client.challenge(rival, proposal)
      preferOnline(Mode.CLASSIC)
      nav.backTo(Screen.NewClassic)
    }
  }
}

@Composable
internal fun NewOnlineDuplicateRoute(lexico: Lexico, nav: Navigator) {
  val client = rememberClient(lexico)
  InvolvedGate(client, nav) {
    NewOnlineDuplicateScreen(nav::back) { turn, rounds, live ->
      client.publish(Proposal.Duplicate(turn, rounds, live))
      nav.replace(Screen.OnlinePlay)
    }
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
  StatsDialog(loaded?.getOrNull()?.let(::statsSections), loaded?.exceptionOrNull()?.message, onClose)
}

@Composable
internal fun OnlinePlayRoute(lexico: Lexico, nav: Navigator, themes: ThemeState) {
  val client = rememberClient(lexico)
  DisposableEffect(client) { onDispose { client.stopWatchingOrForget() } }
  val info by client.room.collectAsState()
  val ended by client.ended.collectAsState()
  val status by client.status.collectAsState()
  val current = info
  LaunchedEffect(current, ended) { if (current == null && ended == ADJOURNED) nav.back() }
  when {
    current == null -> AwaitingReconnection(rememberOffline(client) && ended == null) {
      OnlineWaitScreen("En línea", ended ?: "Entrando…", ended != null) { client.forget(); nav.back() }
    }
    current.watching -> Disconnected(status != null && ended == null, nav::home, note = null) {
      ProvidePhotos(rememberOnline(), client.lobby.collectAsState().value) { Watch(client, current, nav, themes) }
    }
    else -> GoneCover(ended == GAME_GONE, { client.forget(); nav.home() }) {
      AdjournedCover(ended == ADJOURNED, { client.forget(); nav.back() }) {
        Disconnected(status != null && ended == null, nav::home, CLOCK_RUNS.takeIf { current.timed }) {
          SeatedGame(client, current, lexico, nav, themes)
        }
      }
    }
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
  val photo = rememberChosenPhoto { error = it }
  val submit = { signingUp: Boolean, input: AccountInput ->
    scope.launch {
      busy = true
      error = with(online.account) { if (signingUp) signUp(input.form()) else logIn(input.form()) }
      if (error == null && signingUp) photo.base64?.let { online.setPhoto(it) }
      busy = false
    }
    Unit
  }
  OnlineAccountScreen(busy, error, photo.image, photo.pick, nav::back, { submit(true, it) }) { submit(false, it) }
}

private class ChosenPhoto(val base64: String?, val image: ImageBitmap?, val pick: () -> Unit)

@Composable
private fun rememberChosenPhoto(onFailed: (String) -> Unit): ChosenPhoto {
  val scope = rememberCoroutineScope()
  val context = LocalContext.current
  var base64 by remember { mutableStateOf<String?>(null) }
  val picker = rememberLauncherForActivityResult(PickVisualMedia()) { uri ->
    uri?.let { scope.launch { shrunkPhoto(context, it)?.let { chosen -> base64 = chosen } ?: onFailed("No se pudo leer esa foto.") } }
  }
  val image = remember(base64) { base64?.let(::decodePhoto) }
  return ChosenPhoto(base64, image) { picker.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly)) }
}

private fun decodePhoto(base64: String): ImageBitmap? = runCatching {
  val bytes = Base64.decode(base64, Base64.DEFAULT)
  BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
}.getOrNull()

@Composable
internal fun OnlinePanelHost(session: OnlineSession, mode: Mode, online: Online, lexico: Lexico, settings: Settings, nav: Navigator) {
  val client = rememberClient(lexico)
  LaunchedEffect(session.name) { syncBackupOnce(online, settings) }
  val lobby by client.lobby.collectAsState()
  val room by client.room.collectAsState()
  val game by client.game.collectAsState()
  val status by client.status.collectAsState()
  val refused by client.refused.collectAsState()
  JumpWhenSeated(room?.id) { nav.go(Screen.OnlinePlay) }
  val composer = remember { Composer() }
  val duplicate = mode == Mode.DUPLICATE
  val state = OnlinePanelState(
    refused ?: status, room != null && game?.state?.value?.result == null,
    lobby.seeks.filter { it.duplicate == duplicate }.map(::seekItem), classic = !duplicate,
    games = lobby.games.filter { it.duplicate == duplicate }.sortedBy { it.id.hashCode() xor peopleSeed }.map(::gameItem),
    adjourned = lobby.adjourned.filter { it.duplicate == duplicate }
      .map { AdjournedItem(it.id, it.opponent, it.country, it.online, it.busy, it.result, it.duplicate, it.untimed) },
    people = shuffledPeople(lobby.people).map { personItem(it, lobby) },
  )
  ProvidePhotos(online, lobby) {
    ProfileHost(online, ProfileContext(client, composer, nav::go)) {
      AwaitingReconnection(rememberOffline(client) && refused == null) { OnlinePanel(state, panelActions(client, lobby, nav, composer)) }
      if (room == null) MessageDialogs(client, contacts(lobby, emptyList(), emptyList()), null, composer)
    }
  }
}

@Composable
private fun rememberOffline(client: OnlineClient): Boolean {
  val connected by client.connected.collectAsState()
  val offline by produceState(false, connected) {
    if (connected) value = false else { delay(OFFLINE_GRACE_MS); value = true }
  }
  return offline
}

private const val OFFLINE_GRACE_MS = 1_500L

@Composable
private fun JumpWhenSeated(roomId: String?, jump: () -> Unit) {
  var seen by remember { mutableStateOf(roomId) }
  LaunchedEffect(roomId) {
    if (roomId != null && roomId != seen) jump()
    seen = roomId
  }
}

private fun panelActions(client: OnlineClient, lobby: Lobby, nav: Navigator, composer: Composer): OnlinePanelActions {
  val seekOf = { id: String -> (lobby.seeks + lobby.invites).find { it.id == id } }
  return OnlinePanelActions(
    resume = { nav.go(Screen.OnlinePlay) },
    seekClassic = { nav.go(Screen.OnlineNewClassic()) }, createDuplicate = { nav.go(Screen.OnlineNewDuplicate) },
    accept = { id -> seekOf(id)?.let { client.accept(it); nav.go(Screen.OnlinePlay) } },
    cancel = { id -> seekOf(id)?.let(client::cancel) }, start = { id -> seekOf(id)?.let { client.startSeek(it); nav.go(Screen.OnlinePlay) } },
    watch = { id -> lobby.games.find { it.id == id }?.let { client.watch(it); nav.go(Screen.OnlinePlay) } },
    resumeAdjourned = { id -> lobby.adjourned.find { it.id == id }?.let { client.resume(it); nav.go(Screen.OnlinePlay) } },
    seenAdjourned = { id -> lobby.adjourned.find { it.id == id }?.let(client::seen) },
    challenge = { nav.go(Screen.OnlineNewClassic(Person(it.name, it.country, it.busy))) },
    write = { composer.write(ChatContact(it.name, it.country, ContactGroup.OTHERS)) },
    finished = { nav.go(Screen.FinishedFolder(GameFolder.ONLINE)) },
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
  val result = game?.state?.collectAsState()?.value?.result
  val over = result != null
  LaunchedEffect(result?.recordPath) { result?.recordPath?.takeIf { it.isNotEmpty() }?.let(lexico.onlineRecords::add) }
  val away = if (info.timed) RIVAL_AWAY + RIVAL_CLOCK else RIVAL_AWAY
  val shown = status ?: ended ?: away.takeUnless { rivalOnline || over } ?: notice
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
        onAdjourn = { adjourning = true }.takeIf { info.players.size == 2 }, onAbandon = leave, online = true,
      )
      if (info.players.size == 2) AdjournDialogs(client, info, ended != null, adjourning) { adjourning = false }
    }
    ended != null -> OnlineWaitScreen("Sala de ${info.host}", ended.orEmpty(), failed = true) { client.forget(); nav.back() }
    else -> LockedLobby { OnlineLobby(client, info, status, leave) }
  }
}

@Composable
private fun LockedLobby(content: @Composable () -> Unit) {
  BackHandler {}
  CompositionLocalProvider(LocalHomeButton provides null, content = content)
}

@Composable
private fun OnlineLobby(client: OnlineClient, info: RoomInfo, status: String?, leave: () -> Unit) {
  if (info.first) HostLobbyScreen(info.me, info.players, null, client::start, null, wifi = false, exit = LobbyExit(CANCEL_ROOM, CANCEL_ASK, leave))
  else GuestLobbyScreen(info.host, info.players, status, info.me, LobbyExit(LEAVE_ROOM, LEAVE_ASK, leave), null)
}

private const val CANCEL_ROOM = "Cancelar partida"
private const val CANCEL_ASK = "¿Seguro que quieres cancelar la partida?"
private const val LEAVE_ROOM = "Salir de la sala"
private const val LEAVE_ASK = "¿Seguro que quieres salir de la sala?"

@Composable
private fun WithChat(client: OnlineClient, info: RoomInfo, content: @Composable () -> Unit) {
  val composer = remember { Composer() }
  val lobby by client.lobby.collectAsState()
  val watchers by client.watchers.collectAsState()
  val rivals = info.players.filter { it != info.me }
  val list = listOfNotNull(ROOM_CONTACT.takeIf { info.duplicate && rivals.isNotEmpty() }) + contacts(lobby, rivals, watchers)
  val fallback = remember(info.id, list.isEmpty()) { list.filter { it.group == ContactGroup.RIVALS }.randomOrNull() }
  val entry = remember { ChatEntry { composer.write() } }
  CompositionLocalProvider(LocalChat provides entry) {
    ProfileHost(rememberOnline(), ProfileContext(client, composer, {}, inGame = true)) { content(); MessageDialogs(client, list, fallback, composer) }
  }
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
internal fun ProvidePhotos(online: Online, lobby: Lobby, content: @Composable () -> Unit) {
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
internal fun rememberClient(lexico: Lexico): OnlineClient {
  val online = rememberOnline()
  val client = remember { online.client(lexico) }
  DisposableEffect(client) { client.acquire(); onDispose { client.release() } }
  return client
}

@Composable
internal fun rememberOnline(): Online {
  val context = LocalContext.current
  return remember { Online.of(context) }
}

private suspend fun syncBackupOnce(online: Online, settings: Settings) {
  if (backupSynced) return
  online.syncBackup(settings.statsSince)?.let { settings.adoptStatsSince(it); backupSynced = true }
}

private fun gameItem(g: LiveGame): GameItem {
  val names = g.players.zip(g.countries) { name, country -> "$name ${flagOf(country)}" }
  return if (g.duplicate) GameItem(g.id, duplicateTitle(g.players), duplicateDetail(g), g.watchers, duplicate = duplicateLine(g))
  else GameItem(g.id, names.joinToString("  vs  "), classicDetail(g), g.watchers, sides(g), scoreOf(g))
}

private fun duplicateTitle(players: List<String>): String = when {
  players.size <= 2 -> players.joinToString(" y ")
  else -> "${players[0]}, ${players[1]} y ${players.size - 2} más"
}

private fun duplicateLine(g: LiveGame): DuplicateLine =
  DuplicateLine(g.players.size, if (g.leader.isEmpty()) 0 else g.round, g.leader, countryOf(g, g.leader), g.tied, g.efficiency)

private fun countryOf(g: LiveGame, name: String): String = g.countries.getOrElse(g.players.indexOf(name)) { "" }

private fun duplicateDetail(g: LiveGame): String = when {
  g.round == 0 -> "Duplicada · ${g.players.size} jugadores"
  g.efficiency == 0 -> "Ronda ${g.round}"
  else -> "Ronda ${g.round} · líder ${g.efficiency} %"
}

private fun sides(g: LiveGame): Pair<GameSide, GameSide>? =
  g.players.zip(g.countries, ::GameSide).takeIf { it.size == 2 }?.let { it[0] to it[1] }

private fun scoreOf(g: LiveGame): String = if (g.scores.size == 2) "${g.scores[0]} – ${g.scores[1]}" else "vs"

private fun classicDetail(g: LiveGame): String =
  if (g.scores.size == 2) "${g.scores[0]} – ${g.scores[1]} · ${g.bag} en la bolsa" else "Clásica"

internal fun personItem(p: Person): PersonItem = PersonItem(p.name, p.country, p.busy)

internal fun personItem(p: Person, lobby: Lobby): PersonItem = personItem(p).copy(game = gameOf(p, lobby.games), adjourned = adjournedWith(p, lobby)?.id)

internal fun adjournedWith(p: Person, lobby: Lobby): AdjournedGame? =
  lobby.adjourned.find { !it.duplicate && it.result.isEmpty() && it.opponent == p.name && it.country == p.country }

private fun gameOf(p: Person, games: List<LiveGame>): String? =
  games.find { g -> g.players.zip(g.countries).any { (name, country) -> name == p.name && country == p.country } }?.id

private val peopleSeed = Random.nextInt()

internal fun shuffledPeople(people: List<Person>): List<Person> =
  people.sortedWith(compareBy({ it.busy }, { (it.name.lowercase() + "|" + it.country).hashCode() xor peopleSeed }))

internal fun seekItem(s: Seek): SeekItem =
  SeekItem(s.id, s.host, s.country, seekDetail(s), s.mine, s.duplicate, s.players, s.to, s.resume)

private fun seekDetail(s: Seek): String =
  if (s.resume && s.duplicate) "Partida aplazada · duplicada"
  else if (s.resume) "Partida aplazada · ${classicTime(s)}"
  else if (s.duplicate) "👥 ${s.players} en la sala · ${Durations.format(s.turnMs)} por ronda"
  else if (s.mine && s.to.isNotEmpty()) "Reto a ${s.to} · ${classicTime(s)} · ${if (s.single) "Single" else "Void"}"
  else "Clásica · ${classicTime(s)} · ${if (s.single) "Single" else "Void"}"

private fun classicTime(s: Seek): String =
  if (s.timed) "${Durations.format(s.timeMs)} + ${Durations.format(s.overtimeMs)}" else "Sin tiempo"

private fun AccountInput.form(): AccountForm = AccountForm(name, country, password, email)

internal fun statsSections(rows: List<OnlineStatsRow>): List<StatsSection> = rows.sortedBy { it.mode == "duplicate" }.map(::statsSection)

private fun statsSection(row: OnlineStatsRow): StatsSection = if (row.mode == "duplicate") duplicateSection(row) else classicSection(row)

private fun classicSection(row: OnlineStatsRow): StatsSection = StatsSection("Clásica", onlineRows(row))

private fun duplicateSection(row: OnlineStatsRow): StatsSection = StatsSection("Duplicada", onlineRows(row))
