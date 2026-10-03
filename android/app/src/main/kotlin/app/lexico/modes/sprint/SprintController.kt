package app.lexico.modes.sprint

import androidx.compose.runtime.Stable
import app.lexico.game.modes.sprint.SprintGame
import app.lexico.game.modes.sprint.SprintSetup
import app.lexico.model.Placement
import app.lexico.modes.GameController
import app.lexico.ui.sprint.SprintActions
import app.lexico.ui.sprint.SprintConfig
import kotlinx.coroutines.CoroutineScope

@Stable
class SprintController(
  private val game: SprintGame,
  scope: CoroutineScope,
  onExit: () -> Unit,
  private val onRestart: () -> Unit,
) : GameController(game, scope, onExit), SprintActions {
  override fun propose(placement: Placement) = act { game.propose(placement) }

  override fun giveUp() = act { game.giveUp() }

  override fun next() = act { game.next() }

  override fun restart() = onRestart()
}

fun SprintConfig.toSetup(): SprintSetup =
  SprintSetup(totalMs = totalMs, lives = lives, invalidCostsLife = single, difficulty = difficulty.engineName)
