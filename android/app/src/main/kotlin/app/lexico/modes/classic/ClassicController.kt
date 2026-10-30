package app.lexico.modes.classic

import androidx.compose.runtime.Stable
import app.lexico.game.modes.classic.ClassicGame
import app.lexico.game.modes.classic.ClassicSetup
import app.lexico.game.modes.classic.EndgameSetup
import app.lexico.game.modes.classic.LocalPlayers
import app.lexico.model.Placement
import app.lexico.modes.GameController
import app.lexico.ui.classic.ClassicActions
import app.lexico.ui.classic.ClassicConfig
import app.lexico.ui.classic.EndgameConfig
import kotlinx.coroutines.CoroutineScope

@Stable
class ClassicController(
  private val game: ClassicGame,
  scope: CoroutineScope,
  onExit: () -> Unit,
  private val onExport: (recordPath: String) -> Unit,
  private val onReview: (recordPath: String, atEndgame: Boolean) -> Unit,
) : GameController(game, scope, onExit), ClassicActions {
  override fun play(placement: Placement) = act { game.play(placement) }

  override fun exchange(tiles: List<String>) = act { game.exchange(tiles) }

  override fun pass() = act { game.pass() }

  override fun analyze() = review(game.state.value.result?.recordPath) { onReview(it, false) }

  override fun analyzeEndgame() = review(game.state.value.result?.recordPath) { onReview(it, true) }

  override fun export() = review(game.state.value.result?.recordPath, onExport)
}

fun EndgameConfig.toSetup(): EndgameSetup = EndgameSetup(
  minBag = minBag, maxBag = maxBag, minLead = minLead, maxLead = maxLead, q = q.engineName, invalidLosesTurn = single,
  timeMs = if (timed) timeMs else 0, overtimeMs = overtimeMs, starter = starter,
  players = players?.let { LocalPlayers(it.first, it.second) },
)

fun ClassicConfig.toSetup(): ClassicSetup =
  ClassicSetup(
    bot = bot, starter = starter, invalidLosesTurn = single, timeMs = if (timed) timeMs else 0, overtimeMs = overtimeMs, study = study,
    players = players?.let { LocalPlayers(it.first, it.second) },
  )
