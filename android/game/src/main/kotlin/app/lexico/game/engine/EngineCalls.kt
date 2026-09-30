package app.lexico.game.engine

import app.lexico.go.events.Listener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal suspend fun <T> engine(call: () -> T): T = withContext(Dispatchers.IO) { call() }

internal suspend fun <T> stoppingOnCancel(stop: () -> Unit, call: suspend () -> T): T = coroutineScope {
  val watcher = launch { try { awaitCancellation() } finally { stop() } }
  try { call() } finally { watcher.cancel() }
}

internal class EngineListener : Listener {
  @Volatile var target: (() -> Unit)? = null

  override fun onChange() {
    target?.invoke()
  }
}

internal fun Throwable.engineMessage(): String = message ?: toString()
