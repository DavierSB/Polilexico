package app.lexico.modes.duplicate

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.lexico.game.modes.duplicate.DuplicateGame
import app.lexico.game.modes.duplicate.DuplicateSetup
import app.lexico.model.Placement
import app.lexico.ui.duplicate.DuplicateActions
import app.lexico.ui.duplicate.DuplicateConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** Une una duplicada del juego con su pantalla: las acciones de la pantalla van al juego. */
@Stable
class DuplicateController(
  private val game: DuplicateGame,
  private val scope: CoroutineScope,
  private val onExit: () -> Unit,
  private val onReview: (recordPath: String) -> Unit,
) : DuplicateActions {
  /** Lo que el motor contesto a la ultima accion, si la rechazo. */
  var notice: String? by mutableStateOf(null)
    private set

  override fun showRack() = act { game.showRack() }

  override fun propose(placement: Placement) = act { game.propose(placement) }

  override fun pass() = act { game.pass() }

  override fun cancel() = act { game.cancel() }

  override fun resign() {
    game.resign()
    onExit()
  }

  override fun exit() = onExit()

  /** Revisar la partida terminada, si el motor pudo escribir su registro. */
  override fun analyze() {
    val path = game.state.value.recordPath.orEmpty()
    if (path.isEmpty()) notice = "No se pudo guardar el registro de esta partida." else onReview(path)
  }

  override fun pause() = game.pause()

  override fun resume() = game.resume()

  private fun act(call: suspend () -> String?) {
    notice = null
    scope.launch { notice = call() }
  }
}

/** Las opciones de la pantalla de duplicada nueva, como las pide el juego. */
fun DuplicateConfig.toSetup(): DuplicateSetup = DuplicateSetup(invalidLosesTurn = single, turnMs = turnMs)
