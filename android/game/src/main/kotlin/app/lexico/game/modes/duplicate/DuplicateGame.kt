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

class DuplicateGame private constructor(
  private val match: Match,
  id: String,
  saves: SavedGames,
  scope: CoroutineScope,
) : LiveGame<DuplicateState>(id, Mode.DUPLICATE, saves, scope, DuplicateReader(match).read()) {
  val setup: DuplicateSetup = DuplicateSetup(match.game().invalidPlayLosesTurn(), match.turnMs(), match.game().maxRounds().toInt())

  suspend fun showRack(): String? = act { match.showRack() }

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
      open(id, saves, scope) { Duplicate.newMatch(setup.turnMs, setup.invalidLosesTurn, setup.maxRounds.toLong(), it) }

    suspend fun load(text: String, id: String, saves: SavedGames, scope: CoroutineScope): DuplicateGame =
      open(id, saves, scope) { Duplicate.loadMatch(text, it) }

    private suspend fun open(id: String, saves: SavedGames, scope: CoroutineScope, create: (EngineListener) -> Match): DuplicateGame {
      val listener = EngineListener()
      val game = engine { DuplicateGame(create(listener), id, saves, scope) }
      listener.target = game.listen()
      listener.onChange()
      return game
    }
  }
}
