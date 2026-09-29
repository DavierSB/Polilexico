package app.lexico.game.engine

import app.lexico.go.events.Listener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Una llamada al motor, fuera del hilo principal: las del motor bloquean (el bot piensa segundos). */
internal suspend fun <T> engine(call: () -> T): T = withContext(Dispatchers.IO) { call() }

/**
 * `call` (que llama al motor y bloquea), con `stop` para cortarla si se cancela quien espera: la
 * llamada bloqueada no se entera sola de la cancelacion.
 */
internal suspend fun <T> stoppingOnCancel(stop: () -> Unit, call: suspend () -> T): T = coroutineScope {
  val watcher = launch { try { awaitCancellation() } finally { stop() } }
  try { call() } finally { watcher.cancel() }
}

/**
 * El receptor de los avisos del motor, que llegan desde un hilo del motor. Se crea antes que la
 * partida (el motor lo pide al crearla) y se conecta despues con [target]; los avisos de antes se
 * pierden, pero la partida se lee entera al conectarlo.
 */
internal class EngineListener : Listener {
  @Volatile var target: (() -> Unit)? = null

  override fun onChange() {
    target?.invoke()
  }
}

/** El mensaje de un error del motor, para mostrarlo al jugador. */
internal fun Throwable.engineMessage(): String = message ?: toString()
