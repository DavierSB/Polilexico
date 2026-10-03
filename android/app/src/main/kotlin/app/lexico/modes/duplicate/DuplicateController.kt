package app.lexico.modes.duplicate

import androidx.compose.runtime.Stable
import app.lexico.game.modes.duplicate.DuplicateGame
import app.lexico.game.modes.duplicate.DuplicateSetup
import app.lexico.model.Placement
import app.lexico.modes.GameController
import app.lexico.ui.duplicate.DuplicateActions
import app.lexico.ui.duplicate.DuplicateConfig
import kotlinx.coroutines.CoroutineScope

@Stable
class DuplicateController(
  private val game: DuplicateGame,
  scope: CoroutineScope,
  onExit: () -> Unit,
  private val onReview: (recordPath: String) -> Unit,
) : GameController(game, scope, onExit), DuplicateActions {
  override fun showRack() = act { game.showRack() }

  override fun propose(placement: Placement) = act { game.propose(placement) }

  override fun pass() = act { game.pass() }

  override fun cancel() = act { game.cancel() }

  override fun analyze() = review(game.state.value.recordPath, onReview)
}

fun DuplicateConfig.toSetup(): DuplicateSetup = DuplicateSetup(invalidLosesTurn = single, turnMs = turnMs, maxRounds = rounds)
