package app.lexico.modes.classic

import androidx.compose.runtime.Stable
import app.lexico.game.modes.classic.ClassicGame
import app.lexico.game.modes.classic.ClassicSetup
import app.lexico.game.modes.classic.EndgameSetup
import app.lexico.model.Placement
import app.lexico.modes.GameController
import app.lexico.ui.classic.ClassicActions
import app.lexico.ui.classic.ClassicConfig
import app.lexico.ui.classic.EndgameConfig
import kotlinx.coroutines.CoroutineScope

/** Une una partida clasica del juego con su pantalla: las acciones de la pantalla van al juego. */
@Stable
class ClassicController(
  private val game: ClassicGame,
  scope: CoroutineScope,
  onExit: () -> Unit,
  private val onReview: (recordPath: String) -> Unit,
) : GameController(game, scope, onExit), ClassicActions {
  override fun play(placement: Placement) = act { game.play(placement) }

  override fun exchange(tiles: List<String>) = act { game.exchange(tiles) }

  override fun pass() = act { game.pass() }

  override fun analyze() = review(game.state.value.result?.recordPath, onReview)
}

/** Las opciones de Finales, como las pide el juego (tiempo 0 = sin tiempo). */
fun EndgameConfig.toSetup(): EndgameSetup = EndgameSetup(
  maxBag = maxBag, minLead = minLead, maxLead = maxLead, invalidLosesTurn = single,
  timeMs = if (timed) timeMs else 0, overtimeMs = overtimeMs,
)

/** Las opciones de la pantalla de partida nueva, como las pide el juego (tiempo 0 = sin tiempo). */
fun ClassicConfig.toSetup(): ClassicSetup =
  ClassicSetup(
    bot = bot, invalidLosesTurn = single, timeMs = if (timed) timeMs else 0, overtimeMs = overtimeMs,
  )
