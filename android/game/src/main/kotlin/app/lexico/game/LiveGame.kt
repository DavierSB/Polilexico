package app.lexico.game

import app.lexico.game.engine.engine
import app.lexico.game.engine.engineMessage
import app.lexico.game.storage.Mode
import app.lexico.game.storage.SavedGames
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** Cada cuanto se refresca un reloj en marcha, solo para verlo correr en pantalla. */
private const val TICK_MS = 200L

/**
 * Una partida en marcha del motor, vista desde Android: publica su estado [S], lo vuelve a leer
 * con cada aviso del motor y la guarda en disco tras cada cambio. Mientras corre un reloj,
 * refresca solo el tiempo cada poco, para dibujarlo.
 *
 * El motor decide todo (turnos, relojes, bot); aqui solo se le envian las acciones y se pausa la
 * partida cuando deja de verse. Con [mode] null (los minijuegos) no se guarda: al salir se acaba.
 */
abstract class LiveGame<S> internal constructor(
  val id: String,
  private val mode: Mode?,
  private val saves: SavedGames,
  private val scope: CoroutineScope,
  initial: S,
) {
  private val _state = MutableStateFlow(initial)
  val state: StateFlow<S> = _state.asStateFlow()

  @OptIn(ExperimentalCoroutinesApi::class)
  private val serial = Dispatchers.IO.limitedParallelism(1)
  private var ticker: Job? = null
  private var closed = false

  /** Para los relojes (la partida deja de verse, o el jugador pulso pausa). */
  fun pause() {
    scope.launch(serial) { pauseMatch() }
  }

  /** Sigue tras una pausa: solo cuando el jugador pulsa "Continuar". */
  fun resume() {
    scope.launch(serial) { resumeMatch() }
  }

  /** Sale de la partida: queda pausada y guardada para continuarla (si no termino). */
  fun close() {
    scope.launch(serial) { shutDown(keep = !isOver(_state.value)) }
  }

  /** Abandona la partida: se borra de las partidas en curso. */
  fun resign() {
    scope.launch(serial) { shutDown(keep = false) }
  }

  /** Conecta los avisos del motor y lee el estado por primera vez. */
  internal fun listen(): () -> Unit = { scope.launch(serial) { refresh() } }

  /** Una accion del jugador; devuelve el mensaje de error del motor, o null si fue bien. */
  protected suspend fun act(call: () -> Unit): String? =
    engine { runCatching(call).exceptionOrNull()?.engineMessage() }

  protected abstract fun read(): S

  /** El estado con el tiempo al dia (lo unico que cambia entre avisos). */
  protected abstract fun tick(state: S): S

  protected abstract fun isTicking(state: S): Boolean

  protected abstract fun isOver(state: S): Boolean

  protected abstract fun pauseMatch()

  protected abstract fun resumeMatch()

  protected abstract fun saveMatch(): String

  protected abstract fun closeMatch()

  /** Detiene la partida una sola vez: guardada para continuarla (`keep`) o borrada. */
  private fun shutDown(keep: Boolean) {
    if (closed) return
    closed = true
    stopTicker()
    pauseMatch()
    if (keep) save() else forget()
    closeMatch()
  }

  /** Tras un aviso: el estado entero, guardarla (o borrarla si termino) y el reloj en pantalla. */
  private fun refresh() {
    if (closed) return
    val fresh = read()
    _state.value = fresh
    if (isOver(fresh)) forget() else save()
    updateTicker(fresh)
  }

  private fun save() {
    val mode = mode ?: return
    runCatching { saves.write(id, mode, saveMatch()) }
  }

  private fun forget() {
    mode?.let { saves.delete(id, it) }
  }

  private fun updateTicker(state: S) {
    if (!isTicking(state)) return stopTicker()
    if (ticker?.isActive != true) ticker = scope.launch(serial) { tickWhileRunning() }
  }

  private fun stopTicker() {
    ticker?.cancel()
    ticker = null
  }

  private suspend fun CoroutineScope.tickWhileRunning() {
    while (isActive && isTicking(_state.value)) {
      delay(TICK_MS)
      _state.value = tick(_state.value)
    }
  }
}
