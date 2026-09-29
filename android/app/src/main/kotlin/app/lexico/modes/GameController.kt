package app.lexico.modes

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.lexico.game.LiveGame
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Lo que comparten los controladores de las partidas: pasan las acciones de la pantalla al
 * [game], guardan lo que contesto el motor si las rechazo, y pausan, siguen, abandonan y salen.
 * Cada modo solo añade sus propias acciones.
 */
@Stable
abstract class GameController(
  private val game: LiveGame<*>,
  private val scope: CoroutineScope,
  private val onExit: () -> Unit,
) {
  /** Lo que el motor contesto a la ultima accion, si la rechazo. */
  var notice: String? by mutableStateOf(null)
    private set

  fun exit() = onExit()

  fun pause() = game.pause()

  fun resume() = game.resume()

  /** Abandona la partida (se borra de las partidas en curso) y sale. */
  fun resign() {
    game.resign()
    onExit()
  }

  /** Manda una accion al juego; si el motor la rechaza, su motivo queda en [notice]. */
  protected fun act(call: suspend () -> String?) {
    notice = null
    scope.launch { notice = call() }
  }

  /** Abre la revision de la partida terminada, si el motor pudo escribir su registro. */
  protected fun review(recordPath: String?, onReview: (recordPath: String) -> Unit) {
    if (recordPath.isNullOrEmpty()) notice = "No se pudo guardar el registro de esta partida." else onReview(recordPath)
  }
}
