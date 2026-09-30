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

private const val TICK_MS = 200L

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

  fun pause() {
    scope.launch(serial) { pauseMatch() }
  }

  fun resume() {
    scope.launch(serial) { resumeMatch() }
  }

  fun close() {
    scope.launch(serial) { shutDown(keep = !isOver(_state.value)) }
  }

  fun resign() {
    scope.launch(serial) { shutDown(keep = false) }
  }

  internal fun listen(): () -> Unit = { scope.launch(serial) { refresh() } }

  protected suspend fun act(call: () -> Unit): String? =
    engine { runCatching(call).exceptionOrNull()?.engineMessage() }

  protected abstract fun read(): S

  protected abstract fun tick(state: S): S

  protected abstract fun isTicking(state: S): Boolean

  protected abstract fun isOver(state: S): Boolean

  protected abstract fun pauseMatch()

  protected abstract fun resumeMatch()

  protected abstract fun saveMatch(): String

  protected abstract fun closeMatch()

  private fun shutDown(keep: Boolean) {
    if (closed) return
    closed = true
    stopTicker()
    pauseMatch()
    if (keep) save() else forget()
    closeMatch()
  }

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
