package app.lexico.modes.sprint

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.lexico.game.modes.sprint.SprintGame
import app.lexico.game.modes.sprint.SprintSetup
import app.lexico.model.Placement
import app.lexico.ui.sprint.SprintActions
import app.lexico.ui.sprint.SprintConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** Une una serie de Scrabble Sprint del juego con su pantalla: las acciones de la pantalla van al juego. */
@Stable
class SprintController(
  private val game: SprintGame,
  private val scope: CoroutineScope,
  private val onExit: () -> Unit,
  private val onRestart: () -> Unit,
) : SprintActions {
  /** Lo que el motor contesto a la ultima accion, si la rechazo. */
  var notice: String? by mutableStateOf(null)
    private set

  override fun propose(placement: Placement) = act { game.propose(placement) }

  override fun giveUp() = act { game.giveUp() }

  override fun next() = act { game.next() }

  override fun restart() = onRestart()

  override fun exit() = onExit()

  override fun pause() = game.pause()

  override fun resume() = game.resume()

  private fun act(call: suspend () -> String?) {
    notice = null
    scope.launch { notice = call() }
  }
}

/** Las opciones de la pantalla de serie nueva, como las pide el juego. */
fun SprintConfig.toSetup(): SprintSetup = SprintSetup(totalMs = totalMs, lives = lives)
