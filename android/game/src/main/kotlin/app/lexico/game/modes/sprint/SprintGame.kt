package app.lexico.game.modes.sprint

import app.lexico.game.CueReading
import app.lexico.game.LiveGame
import app.lexico.game.engine.EngineListener
import app.lexico.game.engine.engine
import app.lexico.game.storage.SavedGames
import app.lexico.go.sprint.Match
import app.lexico.go.sprint.Sprint
import app.lexico.model.Placement
import kotlinx.coroutines.CoroutineScope

class SprintGame private constructor(
  private val match: Match,
  id: String,
  saves: SavedGames,
  scope: CoroutineScope,
) : LiveGame<SprintState>(id, null, saves, scope, SprintReader(match).read()) {
  suspend fun propose(placement: Placement): String? = act { match.propose(placement.toString()) }

  suspend fun giveUp(): String? = act { match.giveUp() }

  suspend fun next(): String? = act { match.next() }

  override fun read(): SprintState = SprintReader(match).read()

  override fun tick(state: SprintState): SprintState = state.copy(phase = SprintReader(match).phase())

  override fun isTicking(state: SprintState): Boolean = !state.paused && state.phase is SprintPhase.Solving

  override fun isOver(state: SprintState): Boolean = state.phase is SprintPhase.Finished

  override fun lastCue(): CueReading = CueReading(match.cueCount(), match.cue())

  override fun pauseMatch() = match.pause()

  override fun resumeMatch() = match.resume()

  override fun saveMatch(): String = ""

  override fun closeMatch() = match.close()

  internal companion object {
    suspend fun start(setup: SprintSetup, id: String, saves: SavedGames, scope: CoroutineScope): SprintGame {
      val listener = EngineListener()
      val game = engine { SprintGame(Sprint.newMatch(setup.totalMs, setup.lives.toLong(), setup.invalidCostsLife, setup.difficulty, listener), id, saves, scope) }
      listener.target = game.listen()
      listener.onChange()
      return game
    }
  }
}
