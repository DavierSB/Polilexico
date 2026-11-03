package app.lexico.online

import app.lexico.game.Lexico
import app.lexico.game.modes.classic.ClassicGame
import app.lexico.game.modes.classic.ClassicRemote
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val LINGER_MS = 15_000L
private const val FINISHED_PHASE = "\"Phase\":\"finished\""

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
    forget()
    send(create(p))
  }

  fun challenge(person: Person, p: Proposal.Classic) {
    forget()
    send(challengeMessage(person.name, person.country, p))
  }

  fun accept(seek: Seek) {
    forget()
    send(roomAction("join", seek.id))
  }

  fun decline(seek: Seek) = send(roomAction("decline", seek.id))

  fun start() = send(signal("start"))

  fun ready() = send(signal("ready"))

  fun propose(input: String) {
    _notice.value = null
    send(moveMessage(input))
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
  }

  fun forgetIfFinished() {
    val over = _game.value?.state?.value?.result != null || _table.value?.contains(FINISHED_PHASE) == true
    if (over || _ended.value != null) forget()
  }

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
    link = OnlineLink(online.store.server, online.http, scope, online.account::token, { _status.value = it }, ::handle).also { it.open() }
  }

  @Synchronized
  private fun disconnectIfIdle() {
    if (users > 0) return
    link?.close()
    link = null
  }

  private fun send(text: String) {
    link?.send(text)
  }

  private suspend fun handle(e: OnlineEvent) {
    when (e) {
      OnlineEvent.Welcome -> _refused.value = null
      is OnlineEvent.Refused -> refuse(e.reason)
      is OnlineEvent.Failed -> if (_room.value == null) _ended.value = e.reason else _notice.value = e.reason
      is OnlineEvent.LobbyChanged -> _lobby.value = e.lobby
      is OnlineEvent.Room -> _room.value = e.info
      is OnlineEvent.State -> mirror(e)
      is OnlineEvent.Table -> _table.value = e.view
      is OnlineEvent.Rejected -> _notice.value = e.reason
      is OnlineEvent.Rival -> _rivalOnline.value = e.online
      is OnlineEvent.Closed -> _ended.value = e.reason.ifEmpty { "La sala se cerró." }
    }
  }

  private suspend fun mirror(state: OnlineEvent.State) {
    val game = _game.value
    if (game == null) _game.value = lexico.joinOnline(state.save, state.first, this)
    else if (game.state.value.result == null) game.mirror(state.save, false)
  }

  @Synchronized
  private fun refuse(reason: String) {
    _refused.value = reason
    link?.close()
    link = null
  }
}
