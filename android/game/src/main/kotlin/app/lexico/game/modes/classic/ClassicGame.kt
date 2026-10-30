package app.lexico.game.modes.classic

import app.lexico.game.CueReading
import app.lexico.game.LiveGame
import app.lexico.game.engine.EngineListener
import app.lexico.game.engine.engine
import app.lexico.game.engine.stoppingOnCancel
import app.lexico.game.engine.rackText
import app.lexico.game.storage.Mode
import app.lexico.game.storage.SavedGames
import app.lexico.go.classic.Classic
import app.lexico.go.classic.EndgameSearch
import app.lexico.go.classic.Match
import app.lexico.model.Placement
import kotlinx.coroutines.CoroutineScope

enum class Seat { SHARED, HOST, GUEST }

interface ClassicRemote {
  fun move(input: String)
  fun pause()
  fun resume()
}

class ClassicGame private constructor(
  @Volatile private var match: Match,
  id: String,
  mode: Mode?,
  saves: SavedGames,
  scope: CoroutineScope,
  val seat: Seat,
  private val remote: ClassicRemote?,
) : LiveGame<ClassicState>(id, mode, saves, scope, ClassicReader(match, seat).read()) {
  val opponent: String = match.game().opponent()

  val study: Boolean = match.study()

  val host: String? = match.game().takeIf { it.local() }?.host()

  suspend fun play(placement: Placement): String? = move(placement.toString())

  suspend fun exchange(tiles: List<String>): String? = move("cambiar " + rackText(tiles))

  suspend fun pass(): String? = move("pasar")

  suspend fun guestMove(input: String): String? = playAs(rival = true, input)

  suspend fun snapshot(): String = engine { match.save() }

  suspend fun mirror(text: String, paused: Boolean) {
    val listener = EngineListener()
    val fresh = engine { Classic.loadMirror(text, paused, listener) }
    val old = match
    match = fresh
    listener.target = listen()
    engine { old.close() }
    listener.onChange()
  }

  override fun read(): ClassicState = ClassicReader(match, seat).read()

  override fun tick(state: ClassicState): ClassicState = state.copy(clocks = ClassicReader(match, seat).clocks(state.flipped))

  override fun isTicking(state: ClassicState): Boolean = state.clocks?.running != null

  override fun isOver(state: ClassicState): Boolean = state.result != null

  override fun lastCue(): CueReading = CueReading(match.cueCount(), match.cue())

  override fun pauseMatch() = remote?.pause() ?: match.pause()

  override fun resumeMatch() = remote?.resume() ?: match.resume()

  override fun saveMatch(): String = match.save()

  override fun closeMatch() = match.close()

  private suspend fun move(input: String): String? = when (seat) {
    Seat.SHARED -> act { match.play(input) }
    Seat.HOST -> playAs(rival = false, input)
    Seat.GUEST -> null.also { remote?.move(input) }
  }

  private suspend fun playAs(rival: Boolean, input: String): String? = act {
    check(match.game().status().humanToMove != rival) { "No es tu turno" }
    match.play(input)
  }

  internal companion object {
    suspend fun start(setup: ClassicSetup, id: String, saves: SavedGames, scope: CoroutineScope): ClassicGame =
      open(id, Mode.CLASSIC, saves, scope) { newMatch(setup, it).also { m -> m.setStudy(setup.study) } }

    suspend fun startHost(setup: ClassicSetup, id: String, saves: SavedGames, scope: CoroutineScope): ClassicGame =
      open(id, null, saves, scope, Seat.HOST) { newMatch(setup, it) }

    suspend fun joinGuest(text: String, paused: Boolean, remote: ClassicRemote, id: String, saves: SavedGames, scope: CoroutineScope): ClassicGame =
      open(id, null, saves, scope, Seat.GUEST, remote) { Classic.loadMirror(text, paused, it) }

    suspend fun startEndgame(setup: EndgameSetup, id: String, saves: SavedGames, scope: CoroutineScope): ClassicGame =
      searchEndgame(setup) { search -> open(id, Mode.ENDGAME, saves, scope) { endgameMatch(search, setup, it) } }

    suspend fun startEndgameHost(setup: EndgameSetup, id: String, saves: SavedGames, scope: CoroutineScope): ClassicGame =
      searchEndgame(setup) { search -> open(id, null, saves, scope, Seat.HOST) { endgameMatch(search, setup, it) } }

    private suspend fun searchEndgame(setup: EndgameSetup, start: suspend (EndgameSearch) -> ClassicGame): ClassicGame {
      val search = Classic.newEndgameSearch(
        setup.minBag.toLong(), setup.maxBag.toLong(), setup.minLead.toLong(), setup.maxLead.toLong(), setup.q,
      )
      return stoppingOnCancel(search::stop) { start(search) }
    }

    private fun endgameMatch(search: EndgameSearch, setup: EndgameSetup, listener: EngineListener): Match {
      val players = setup.players ?: return search.match(setup.timeMs, setup.overtimeMs, setup.invalidLosesTurn, listener)
      return search.localMatch(
        players.first, players.second, setup.starter.toLong(), setup.timeMs, setup.overtimeMs, setup.invalidLosesTurn, listener,
      )
    }

    suspend fun load(text: String, id: String, mode: Mode, saves: SavedGames, scope: CoroutineScope): ClassicGame =
      open(id, mode, saves, scope) { Classic.loadMatch(text, it) }

    private fun newMatch(setup: ClassicSetup, listener: EngineListener): Match {
      val players = setup.players ?: return Classic.newMatch(
        setup.bot, setup.starter.toLong(), setup.timeMs, setup.overtimeMs, setup.invalidLosesTurn, listener,
      )
      return Classic.newLocalMatch(
        players.first, players.second, setup.starter.toLong(), setup.timeMs, setup.overtimeMs, setup.invalidLosesTurn, listener,
      )
    }

    private suspend fun open(
      id: String, mode: Mode?, saves: SavedGames, scope: CoroutineScope, seat: Seat = Seat.SHARED, remote: ClassicRemote? = null,
      create: (EngineListener) -> Match,
    ): ClassicGame {
      val listener = EngineListener()
      val game = engine { ClassicGame(create(listener), id, mode, saves, scope, seat, remote) }
      listener.target = game.listen()
      listener.onChange()
      return game
    }
  }
}
