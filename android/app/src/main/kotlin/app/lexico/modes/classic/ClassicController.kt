package app.lexico.modes.classic

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.lexico.game.modes.classic.ClassicGame
import app.lexico.game.modes.classic.ClassicSetup
import app.lexico.game.modes.classic.EndgameSetup
import app.lexico.model.Placement
import app.lexico.ui.classic.ClassicActions
import app.lexico.ui.classic.ClassicConfig
import app.lexico.ui.classic.EndgameConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** Une una partida clasica del juego con su pantalla: las acciones de la pantalla van al juego. */
@Stable
class ClassicController(
  private val game: ClassicGame,
  private val scope: CoroutineScope,
  private val onExit: () -> Unit,
  private val onReview: (recordPath: String) -> Unit,
) : ClassicActions {
  /** Lo que el motor contesto a la ultima accion, si la rechazo. */
  var notice: String? by mutableStateOf(null)
    private set

  override fun play(placement: Placement) = act { game.play(placement) }

  override fun exchange(tiles: List<String>) = act { game.exchange(tiles) }

  override fun pass() = act { game.pass() }

  override fun resign() {
    game.resign()
    onExit()
  }

  override fun exit() = onExit()

  /** Revisar la partida terminada, si el motor pudo escribir su registro. */
  override fun analyze() {
    val path = game.state.value.result?.recordPath.orEmpty()
    if (path.isEmpty()) notice = "No se pudo guardar el registro de esta partida." else onReview(path)
  }

  override fun pause() = game.pause()

  override fun resume() = game.resume()

  private fun act(call: suspend () -> String?) {
    notice = null
    scope.launch { notice = call() }
  }
}

/** Las opciones de Finales, como las pide el juego (tiempo 0 = sin tiempo). */
fun EndgameConfig.toSetup(): EndgameSetup = EndgameSetup(
  maxBag = maxBag, minLead = minLead, maxLead = maxLead, invalidLosesTurn = single,
  timeMs = if (timed) timeMs else 0, overtimeMs = overtimeMs, showUnseen = showUnseen,
)

/** Las opciones de la pantalla de partida nueva, como las pide el juego (tiempo 0 = sin tiempo). */
fun ClassicConfig.toSetup(): ClassicSetup =
  ClassicSetup(
    bot = bot, invalidLosesTurn = single, timeMs = if (timed) timeMs else 0, overtimeMs = overtimeMs, showUnseen = showUnseen,
  )
