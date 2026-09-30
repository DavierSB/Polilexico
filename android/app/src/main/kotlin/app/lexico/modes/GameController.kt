package app.lexico.modes

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.lexico.game.LiveGame
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Stable
abstract class GameController(
  private val game: LiveGame<*>,
  private val scope: CoroutineScope,
  private val onExit: () -> Unit,
) {
  var notice: String? by mutableStateOf(null)
    private set

  fun exit() = onExit()

  fun pause() = game.pause()

  fun resume() = game.resume()

  fun resign() {
    game.resign()
    onExit()
  }

  protected fun act(call: suspend () -> String?) {
    notice = null
    scope.launch { notice = call() }
  }

  protected fun review(recordPath: String?, onReview: (recordPath: String) -> Unit) {
    if (recordPath.isNullOrEmpty()) notice = "No se pudo guardar el registro de esta partida." else onReview(recordPath)
  }
}
