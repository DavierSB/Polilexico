package app.lexico.online

import android.os.SystemClock
import app.lexico.game.Lexico
import app.lexico.game.modes.classic.ClassicGame
import app.lexico.game.modes.classic.ClassicRemote
import app.lexico.ui.common.ChatLine
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
private const val GAME_GONE = "La partida ya no está disponible."
private const val NOTICE_MS = 5_000L
const val ADJOURNED = "Partida aplazada."

class OnlineClient internal constructor(private val online: Online, private val lexico: Lexico) : ClassicRemote {
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
  private var link: OnlineLink? = null
  private var users = 0
  private var lingering: Job? = null
  private val _status = MutableStateFlow<String?>("Conectando…")
  val status: StateFlow<String?> = _status.asStateFlow()
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

  private val _rejoined = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
  val rejoined: SharedFlow<Unit> = _rejoined

  private var noticeJob: Job? = null
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
    _ended.value = null
    send(create(p))
  }

  fun challenge(person: Person, p: Proposal.Classic) {
    _ended.value = null
    send(challengeMessage(person.name, person.country, p))
  }

  fun cancel(seek: Seek) {
    send(roomAction("cancel", seek.id))
  }

  fun startSeek(seek: Seek) {
    forget()
    send(roomAction("start", seek.id))
  }

  fun accept(seek: Seek) {
    forget()
    send(roomAction("join", seek.id))
  }

  fun decline(seek: Seek) {
    send(roomAction("decline", seek.id))
  }

  fun watch(game: LiveGame) {
    forget()
    send(roomAction("watch", game.id))
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
    send(roomAction("resume", game.id))
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
    send(tellMessage(to.name, to.country, text))
  }

  fun ignorePerson(name: String, country: String) {
    online.store.ignored = online.store.ignored + personKey(name, country)
    _inbox.value = _inbox.value.filterNot { it.from == name && it.country == country }
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
    forget()
    _lobby.value = Lobby()
  }

  override fun move(input: String) = propose(input)

  override fun pause() {}

  override fun resume() {}

  override fun resign() = leave()

  private fun connect() {
    _refused.value = null
    link = OnlineLink(online.store.server, online.http, online.network, scope, online.account::token, { online.account.session.value != null }, { _status.value = it }, ::handle)
      .also { it.open() }
  }

  @Synchronized
  private fun disconnectIfIdle() {
    if (users > 0 || seatedInGame()) return
    link?.close()
    link = null
  }

  private fun send(text: String): Boolean = link?.send(text) == true

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
      is OnlineEvent.Chat -> heard(ChatLine(e.from, e.text))
      is OnlineEvent.Tell -> if (personKey(e.name, e.country) !in online.store.ignored) heard(ChatLine(e.name, e.text, e.country))
      is OnlineEvent.Watchers -> _watchers.value = e.people
    }
  }

  private fun closed(reason: String) {
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
    awaitingRoom = true
    _room.value?.takeIf { it.watching }?.let { send(roomAction("watch", it.id)) }
  }

  private fun seated(info: RoomInfo) {
    if (_room.value?.id != info.id) forget()
    _room.value = info
    awaitingRoom = false
    awaitingGame = !info.watching
  }

  private fun lobbyChanged(lobby: Lobby) {
    _lobby.value = lobby
    if (awaitingRoom && seatedInGame()) _ended.value = GAME_GONE
    awaitingRoom = false
  }

  private fun rejoinIfLive() {
    if (awaitingGame && !finished()) _rejoined.tryEmit(Unit)
    awaitingGame = false
  }

  private fun personKey(name: String, country: String): String = name.lowercase() + "|" + country

  private fun heard(line: ChatLine) {
    if (!_ignoring.value) _inbox.value = (_inbox.value + line).takeLast(INBOX_SIZE)
  }

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
  }
}
