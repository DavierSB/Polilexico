package app.lexico.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import app.lexico.game.LiveGame

@Composable
fun PauseWhenHidden(game: LiveGame<*>) {
  LifecycleEventEffect(Lifecycle.Event.ON_STOP) { game.pause() }
  DisposableEffect(game) { onDispose { game.close() } }
}

@Composable
fun <T> rememberCreated(key: Any, create: suspend () -> T): Result<T>? {
  val result by produceState<Result<T>?>(null, key) { value = runCatching { create() } }
  return result
}

@Composable
fun <T> WhenReady(result: Result<T>?, waiting: String, content: @Composable (T) -> Unit) {
  when {
    result == null -> Waiting(waiting)
    result.isFailure -> Failed(result.exceptionOrNull())
    else -> content(result.getOrThrow())
  }
}

@Composable
private fun Waiting(text: String) {
  Column(Modifier.fillMaxSize(), Arrangement.spacedBy(16.dp, Alignment.CenterVertically), Alignment.CenterHorizontally) {
    CircularProgressIndicator()
    Text(text)
  }
}

@Composable
private fun Failed(error: Throwable?) {
  Text("No se pudo preparar: ${error?.message ?: error}", color = MaterialTheme.colorScheme.error)
}
