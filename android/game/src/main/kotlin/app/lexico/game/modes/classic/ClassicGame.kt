package app.lexico.game.modes.classic

import app.lexico.game.LiveGame
import app.lexico.game.engine.EngineListener
import app.lexico.game.engine.engine
import app.lexico.game.engine.stoppingOnCancel
import app.lexico.game.engine.rackText
import app.lexico.game.storage.Mode
import app.lexico.game.storage.SavedGames
import app.lexico.go.classic.Classic
import app.lexico.go.classic.Match
import app.lexico.model.Placement
import kotlinx.coroutines.CoroutineScope

/**
 * Una partida clasica en marcha contra un bot. El motor lleva los turnos, los relojes y el bot,
 * que juega solo cuando le toca (y, si la partida esta en pausa, al continuar). Finales
 * ([Mode.ENDGAME]) es tambien una clasica, que empieza en un final.
 */
class ClassicGame private constructor(
  private val match: Match,
  id: String,
  mode: Mode,
  saves: SavedGames,
  scope: CoroutineScope,
) : LiveGame<ClassicState>(id, mode, saves, scope, ClassicReader(match).read()) {
  /** El bot rival. */
  val opponent: String = match.game().opponent()

  /** Tu colocacion; devuelve el motivo si el motor la rechaza, o null. */
  suspend fun play(placement: Placement): String? = act { match.play(placement.toString()) }

  suspend fun exchange(tiles: List<String>): String? = act { match.play("cambiar " + rackText(tiles)) }

  suspend fun pass(): String? = act { match.play("pasar") }

  override fun read(): ClassicState = ClassicReader(match).read()

  override fun tick(state: ClassicState): ClassicState = state.copy(clocks = ClassicReader(match).clocks())

  override fun isTicking(state: ClassicState): Boolean = state.clocks?.running != null

  override fun isOver(state: ClassicState): Boolean = state.result != null

  override fun pauseMatch() = match.pause()

  override fun resumeMatch() = match.resume()

  override fun saveMatch(): String = match.save()

  override fun closeMatch() = match.close()

  internal companion object {
    suspend fun start(setup: ClassicSetup, id: String, saves: SavedGames, scope: CoroutineScope): ClassicGame =
      open(id, Mode.CLASSIC, saves, scope) {
        Classic.newMatch(setup.bot, setup.timeMs, setup.overtimeMs, setup.invalidLosesTurn, it)
      }

    /** Busca la partida de Finales (HastyBot contra si mismo) y la empieza; si se cancela, la busqueda se detiene. */
    suspend fun startEndgame(setup: EndgameSetup, id: String, saves: SavedGames, scope: CoroutineScope): ClassicGame {
      val search = Classic.newEndgameSearch(setup.maxBag.toLong(), setup.minLead.toLong(), setup.maxLead.toLong())
      return stoppingOnCancel(search::stop) {
        open(id, Mode.ENDGAME, saves, scope) {
          search.match(setup.timeMs, setup.overtimeMs, setup.invalidLosesTurn, it)
        }
      }
    }

    suspend fun load(text: String, id: String, mode: Mode, saves: SavedGames, scope: CoroutineScope): ClassicGame =
      open(id, mode, saves, scope) { Classic.loadMatch(text, it) }

    /** Crea la partida en el motor y la conecta a sus avisos. */
    private suspend fun open(id: String, mode: Mode, saves: SavedGames, scope: CoroutineScope, create: (EngineListener) -> Match): ClassicGame {
      val listener = EngineListener()
      val game = engine { ClassicGame(create(listener), id, mode, saves, scope) }
      listener.target = game.listen()
      listener.onChange()
      return game
    }
  }
}
