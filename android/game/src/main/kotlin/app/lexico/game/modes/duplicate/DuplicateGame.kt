package app.lexico.game.modes.duplicate

import app.lexico.game.LiveGame
import app.lexico.game.engine.EngineListener
import app.lexico.game.engine.engine
import app.lexico.game.storage.Mode
import app.lexico.game.storage.SavedGames
import app.lexico.go.duplicate.Duplicate
import app.lexico.go.duplicate.Match
import app.lexico.model.Placement
import kotlinx.coroutines.CoroutineScope

/**
 * Una duplicada en marcha contra el master. El motor lleva el ritmo de cada ronda: el reloj del
 * turno, la mano invalida y la ventana para cancelar, que avanzan solos.
 */
class DuplicateGame private constructor(
  private val match: Match,
  id: String,
  saves: SavedGames,
  scope: CoroutineScope,
) : LiveGame<DuplicateState>(id, Mode.DUPLICATE, saves, scope, DuplicateReader(match).read()) {
  /** Ver el atril de la ronda (arranca su reloj). */
  suspend fun showRack(): String? = act { match.showRack() }

  /** Proponer tu colocacion: queda por confirmar unos segundos, en los que se puede cancelar. */
  suspend fun propose(placement: Placement): String? = act { match.propose(placement.toString()) }

  suspend fun pass(): String? = act { match.propose("pasar") }

  suspend fun cancel(): String? = act { match.cancel() }

  override fun read(): DuplicateState = DuplicateReader(match).read()

  override fun tick(state: DuplicateState): DuplicateState = state.copy(phase = DuplicateReader(match).phase())

  override fun isTicking(state: DuplicateState): Boolean =
    !state.paused && (state.phase is DuplicatePhase.Playing || state.phase is DuplicatePhase.Confirming)

  override fun isOver(state: DuplicateState): Boolean = state.phase == DuplicatePhase.Finished

  override fun pauseMatch() = match.pause()

  override fun resumeMatch() = match.resume()

  override fun saveMatch(): String = match.save()

  override fun closeMatch() = match.close()

  internal companion object {
    suspend fun start(setup: DuplicateSetup, id: String, saves: SavedGames, scope: CoroutineScope): DuplicateGame =
      open(id, saves, scope) { Duplicate.newMatch(setup.turnMs, setup.invalidLosesTurn, it) }

    suspend fun load(text: String, id: String, saves: SavedGames, scope: CoroutineScope): DuplicateGame =
      open(id, saves, scope) { Duplicate.loadMatch(text, it) }

    /** Crea la partida en el motor y la conecta a sus avisos. */
    private suspend fun open(id: String, saves: SavedGames, scope: CoroutineScope, create: (EngineListener) -> Match): DuplicateGame {
      val listener = EngineListener()
      val game = engine { DuplicateGame(create(listener), id, saves, scope) }
      listener.target = game.listen()
      listener.onChange()
      return game
    }
  }
}
