package app.lexico.online

import android.os.SystemClock
import app.lexico.game.Lexico
import app.lexico.game.modes.classic.ClassicGame
import app.lexico.game.modes.classic.ClassicRemote
import app.lexico.ui.common.ChatLine
import app.lexico.ui.common.ChatRecord
import app.lexico.ui.common.snoozeResumeReminder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val LINGER_MS = 15_000L
private const val FINISHED_PHASE = "\"Phase\":\"finished\""
private const val INBOX_SIZE = 10
private const val UNSENT = "Sin conexión: la jugada no se envió."
const val GAME_GONE = "La partida ya no está disponible."
private const val NOTICE_MS = 5_000L
const val ADJOURNED = "Partida aplazada."

class OnlineClient internal constructor(private val online: Online, private val lexico: Lexico) : ClassicRemote {
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
  private var link: OnlineLink? = null
  private var users = 0
  private var lingering: Job? = null
  private val _status = MutableStateFlow<String?>("Conectando…")
  val status: StateFlow<String?> = _status.asStateFlow()
  private val _connected = MutableStateFlow(false)
  val connected: StateFlow<Boolean> = _connected.asStateFlow()
  private val _refused = MutableStateFlow<String?>(null)
  val refused: StateFlow<String?> = _refused.asStateFlow()
  private val _lobby = MutableStateFlow(Lobby())
  val lobby: StateFlow<Lobby> = _lobby.asStateFlow()
  private val _room = MutableStateFlow<RoomInfo?>(null)
  val room: StateFlow<RoomInfo?> = _room.asStateFlow()
  private val _game = MutableStateFlow<ClassicGame?>(null)
  val game: StateFlow<ClassicGame?> = _game.asStateFlow()
  private val _table = MutableStateFlow<String?>(null)
  val table: StateFlow<String?> = _table.asStateFlow()
  private val _notice = MutableStateFlow<String?>(null)
  val notice: StateFlow<String?> = _notice.asStateFlow()
  private val _rivalOnline = MutableStateFlow(true)
  val rivalOnline: StateFlow<Boolean> = _rivalOnline.asStateFlow()
  private val _ended = MutableStateFlow<String?>(null)
  val ended: StateFlow<String?> = _ended.asStateFlow()

  private var publishing: Boolean? = null

  private val _inbox = MutableStateFlow<List<ChatLine>>(emptyList())
  val inbox: StateFlow<List<ChatLine>> = _inbox.asStateFlow()

  private val _ignoring = MutableStateFlow(false)
  val ignoring: StateFlow<Boolean> = _ignoring.asStateFlow()

  private val _watchers = MutableStateFlow<List<Person>>(emptyList())
  val watchers: StateFlow<List<Person>> = _watchers.asStateFlow()

  private val _lastTo = MutableStateFlow<Person?>(null)
  val lastTo: StateFlow<Person?> = _lastTo.asStateFlow()

  private val _adjournAsk = MutableStateFlow<String?>(null)
  val adjournAsk: StateFlow<String?> = _adjournAsk.asStateFlow()

  private val _rivalAwaySince = MutableStateFlow<Long?>(null)
  val rivalAwaySince: StateFlow<Long?> = _rivalAwaySince.asStateFlow()

  private val _offer = MutableStateFlow<String?>(null)
  val offer: StateFlow<String?> = _offer.asStateFlow()

  private val _rejoined = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
  val rejoined: SharedFlow<Unit> = _rejoined

  private var noticeJob: Job? = null
  private val queued = mutableListOf<String>()
  private var awaitingRoom = false
  private var awaitingGame = false

  @Synchronized
  fun acquire() {
    users++
    lingering?.cancel()
    if (link == null) connect()
  }

  @Synchronized
  fun release() {
    users--
    if (users == 0) lingering = scope.launch { delay(LINGER_MS); disconnectIfIdle() }
  }

  fun publish(p: Proposal) {
    if (p is Proposal.Duplicate) forget()
    _ended.value = null
    publishing = p is Proposal.Duplicate
    sendWhenOnline(create(p))
  }

  fun challenge(person: Person, p: Proposal.Classic) {
    _ended.value = null
    publishing = false
    sendWhenOnline(challengeMessage(person.name, person.country, p))
  }

  fun hasOwnGame(duplicate: Boolean): Boolean =
    publishing == duplicate || ownSeat(duplicate) || _lobby.value.seeks.any { it.mine && it.duplicate == duplicate }

  private fun ownSeat(duplicate: Boolean): Boolean = _room.value?.let { !it.watching && it.duplicate == duplicate } == true

  fun cancel(seek: Seek) {
    sendWhenOnline(roomAction("cancel", seek.id))
  }

  fun startSeek(seek: Seek) {
    forget()
    sendWhenOnline(roomAction("start", seek.id))
  }

  fun accept(seek: Seek) {
    forget()
    sendWhenOnline(roomAction("join", seek.id))
  }

  fun decline(seek: Seek) {
    sendWhenOnline(roomAction("decline", seek.id))
  }

  fun watch(game: LiveGame) {
    forget()
    sendWhenOnline(roomAction("watch", game.id))
  }

  fun adjourn() {
    send(signal("adjourn"))
  }

  fun answerAdjourn(accept: Boolean) {
    _adjournAsk.value = null
    send(adjournReply(accept))
  }

  fun seen(game: AdjournedGame) {
    send(roomAction("seen", game.id))
  }

  fun resume(game: AdjournedGame) {
    forget()
    sendWhenOnline(roomAction("resume", game.id))
  }

  fun start() {
    send(signal("start"))
  }

  fun ready() {
    send(signal("ready"))
  }

  fun propose(input: String) {
    if (send(moveMessage(input))) _notice.value = null else flash(UNSENT)
  }

  fun tell(to: Person, text: String) {
    _lastTo.value = to
    if (send(tellMessage(to.name, to.country, text))) logged(to.name, to.country, text, mine = true, room = false)
  }

  fun tellRoom(text: String) {
    if (send(roomChatMessage(text))) logged("", "", text, mine = true, room = true)
  }

  fun ignorePerson(name: String, country: String) {
    online.store.ignored = online.store.ignored + personKey(name, country)
    _inbox.value = _inbox.value.filterNot { it.from == name && it.country == country }
  }

  fun dismissOffer() {
    _offer.value = null
  }

  fun closeChat() {
    _inbox.value = emptyList()
  }

  fun ignore(on: Boolean) {
    _ignoring.value = on
    closeChat()
  }

  fun leave() {
    send(signal("leave"))
    forget()
  }

  fun forget() {
    _game.value?.close()
    _room.value = null
    _game.value = null
    _table.value = null
    _notice.value = null
    _ended.value = null
    _rivalOnline.value = true
    _rivalAwaySince.value = null
    _adjournAsk.value = null
    _watchers.value = emptyList()
  }

  fun stopWatchingOrForget() {
    if (_room.value?.watching == true) leave() else forgetIfFinished()
  }

  fun forgetIfFinished() {
    if (_room.value != null && (finished() || _ended.value != null)) forget()
  }

  private fun finished(): Boolean =
    _game.value?.state?.value?.result != null || _table.value?.contains(FINISHED_PHASE) == true

  private fun seatedInGame(): Boolean = _room.value?.watching == false && !finished() && _ended.value == null

  @Synchronized
  fun shutDown() {
    link?.close()
    link = null
    _connected.value = false
    synchronized(queued) { queued.clear() }
    forget()
    _lobby.value = Lobby()
  }

  override fun move(input: String) = propose(input)

  override fun pause() {}

  override fun resume() {}

  override fun resign() = leave()

  private fun connect() {
    _refused.value = null
    link = OnlineLink(online.store.server, online.http, online.network, scope, online.account::token, { online.account.session.value != null }, ::linkStatus, ::handle)
      .also { it.open() }
  }

  @Synchronized
  private fun disconnectIfIdle() {
    if (users > 0 || seatedInGame()) return
    link?.close()
    link = null
    _connected.value = false
  }

  private fun linkStatus(text: String?) {
    _status.value = text
    if (text != null) _connected.value = false
  }

  private fun send(text: String): Boolean = link?.send(text) == true

  private fun sendWhenOnline(text: String) {
    if (!send(text)) synchronized(queued) { queued.remove(text); queued += text }
  }

  private fun sendQueued() {
    val due = synchronized(queued) { queued.toList().also { queued.clear() } }
    due.forEach(::sendWhenOnline)
  }

  private suspend fun handle(e: OnlineEvent) {
    when (e) {
      OnlineEvent.Welcome -> welcomed()
      is OnlineEvent.Refused -> refuse(e.reason)
      is OnlineEvent.Failed -> if (_room.value == null) _ended.value = e.reason else flash(e.reason)
      is OnlineEvent.LobbyChanged -> lobbyChanged(e.lobby)
      is OnlineEvent.Room -> seated(e.info)
      is OnlineEvent.State -> { mirror(e); rejoinIfLive() }
      is OnlineEvent.Table -> { _table.value = e.view; rejoinIfLive() }
      is OnlineEvent.Rejected -> flash(e.reason)
      is OnlineEvent.Rival -> rival(e)
      is OnlineEvent.AdjournAsk -> _adjournAsk.value = e.name
      is OnlineEvent.Closed -> closed(e.reason.ifEmpty { "La sala se cerró." })
      is OnlineEvent.Chat -> heard(ChatLine(e.from, e.text, room = true))
      is OnlineEvent.Tell -> if (personKey(e.name, e.country) !in online.store.ignored) heard(ChatLine(e.name, e.text, e.country))
      is OnlineEvent.Watchers -> _watchers.value = e.people
      is OnlineEvent.DuplicateOffer -> _offer.value = e.code
    }
  }

  private fun closed(reason: String) {
    if (reason == ADJOURNED) _room.value?.id?.let(::snoozeResumeReminder)
    if (reason == ADJOURNED && !(awaitingRoom && seatedInGame())) forget()
    _ended.value = reason
  }

  private fun rival(e: OnlineEvent.Rival) {
    _rivalOnline.value = e.online
    _rivalAwaySince.value = if (e.online) null else SystemClock.elapsedRealtime() - e.awayMs
  }

  private fun flash(text: String) {
    _notice.value = text
    noticeJob?.cancel()
    noticeJob = scope.launch {
      delay(NOTICE_MS)
      if (_notice.value == text) _notice.value = null
    }
  }

  private fun welcomed() {
    _refused.value = null
    _connected.value = true
    awaitingRoom = true
    _room.value?.takeIf { it.watching }?.let { send(roomAction("watch", it.id)) }
    sendQueued()
  }

  private fun seated(info: RoomInfo) {
    if (_room.value?.id != info.id) forget()
    _room.value = info
    awaitingRoom = false
    awaitingGame = !info.watching
  }

  private fun lobbyChanged(lobby: Lobby) {
    _lobby.value = lobby
    publishing = null
    if (awaitingRoom && seatedInGame()) _ended.value = GAME_GONE
    awaitingRoom = false
  }

  private fun rejoinIfLive() {
    if (awaitingGame && !finished()) _rejoined.tryEmit(Unit)
    awaitingGame = false
  }

  private fun personKey(name: String, country: String): String = name.lowercase() + "|" + country

  private fun heard(line: ChatLine) {
    if (_ignoring.value) return
    _inbox.value = (_inbox.value + line).takeLast(INBOX_SIZE)
    logged(line.from, line.country, line.text, mine = false, room = line.room)
  }

  private fun logged(who: String, country: String, text: String, mine: Boolean, room: Boolean) =
    online.chatLog.add(ChatRecord(System.currentTimeMillis(), who, country, text, mine, room))

  private suspend fun mirror(state: OnlineEvent.State) {
    val game = _game.value
    if (game == null) _game.value = lexico.joinOnline(state.save, state.first, this, _room.value?.watching == true)
    else if (game.state.value.result == null) game.mirror(state.save, false)
  }

  @Synchronized
  private fun refuse(reason: String) {
    _refused.value = reason
    link?.close()
    link = null
    _connected.value = false
  }
}
