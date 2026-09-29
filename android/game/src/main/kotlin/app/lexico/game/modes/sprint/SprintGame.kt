package app.lexico.game.modes.sprint

import app.lexico.game.LiveGame
import app.lexico.game.engine.EngineListener
import app.lexico.game.engine.engine
import app.lexico.game.storage.SavedGames
import app.lexico.go.sprint.Match
import app.lexico.go.sprint.Sprint
import app.lexico.model.Placement
import kotlinx.coroutines.CoroutineScope

/**
 * Una serie de Scrabble Sprint en marcha. El motor lleva todo: la busqueda de manos con
 * HastyBot, el reloj de la serie y las vidas. Es un minijuego: no se guarda al salir.
 */
class SprintGame private constructor(
  private val match: Match,
  id: String,
  saves: SavedGames,
  scope: CoroutineScope,
) : LiveGame<SprintState>(id, null, saves, scope, SprintReader(match).read()) {
  /** Tu scrabble; si no lo es (o no es valido), el motor dice por que y la mano sigue. */
  suspend fun propose(placement: Placement): String? = act { match.propose(placement.toString()) }

  /** Rendirse en la mano en juego: cuesta una vida. */
  suspend fun giveUp(): String? = act { match.giveUp() }

  /** De la mano cerrada a la siguiente. */
  suspend fun next(): String? = act { match.next() }

  override fun read(): SprintState = SprintReader(match).read()

  override fun tick(state: SprintState): SprintState = state.copy(phase = SprintReader(match).phase())

  override fun isTicking(state: SprintState): Boolean = !state.paused && state.phase is SprintPhase.Solving

  override fun isOver(state: SprintState): Boolean = state.phase is SprintPhase.Finished

  override fun pauseMatch() = match.pause()

  override fun resumeMatch() = match.resume()

  override fun saveMatch(): String = ""

  override fun closeMatch() = match.close()

  internal companion object {
    /** Crea la serie en el motor y la conecta a sus avisos. */
    suspend fun start(setup: SprintSetup, id: String, saves: SavedGames, scope: CoroutineScope): SprintGame {
      val listener = EngineListener()
      val game = engine { SprintGame(Sprint.newMatch(setup.totalMs, setup.lives.toLong(), listener), id, saves, scope) }
      listener.target = game.listen()
      listener.onChange()
      return game
    }
  }
}
