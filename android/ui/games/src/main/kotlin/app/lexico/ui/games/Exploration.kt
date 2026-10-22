package app.lexico.ui.games

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch

@Composable
internal fun rememberExploration(sessions: EndgameSessions, page: Int): Exploration {
  val scope = rememberCoroutineScope()
  val exploration = remember { Exploration(scope) }
  LaunchedEffect(Unit) { exploration.open { sessions(page) } }
  return exploration
}

@Stable
internal class Exploration(private val scope: CoroutineScope) {
  var position: ExploredPosition? by mutableStateOf(null)
    private set
  var lines: LiveLines by mutableStateOf(LiveLines.NONE)
    private set
  var analyzing by mutableStateOf(false)
    private set
  var failed by mutableStateOf(false)
    private set
  var notice: String? by mutableStateOf(null)

  private var session: EndgameSession? = null
  private var job: Job? = null
  private val finished = mutableMapOf<List<String>, LiveLines>()

  suspend fun open(create: suspend () -> EndgameSession) {
    session = guarded { create() } ?: return run { failed = true }
    refresh()
  }

  fun play(move: String?) {
    scope.launch {
      val error = session?.play(move) ?: return@launch moved()
      notice = error
    }
  }

  fun back(steps: Int = 1) {
    scope.launch {
      session?.back(steps)
      moved()
    }
  }

  private suspend fun moved() {
    job?.cancelAndJoin()
    refresh()
  }

  private suspend fun refresh() {
    val s = session ?: return
    val now = guarded { s.position() } ?: return
    position = now
    val key = keyOf(now)
    lines = finished[key] ?: LiveLines.NONE
    if (key !in finished && !now.over) job = scope.launch { analyze(s, key) }
  }

  private suspend fun analyze(s: EndgameSession, key: List<String>) {
    analyzing = true
    try {
      guarded { s.analyze { lines = it } }?.let { finished[key] = lines }
    } finally {
      analyzing = false
    }
  }
}

private fun keyOf(p: ExploredPosition): List<String> = p.path.map { it.move.text }

private suspend fun <T> guarded(call: suspend () -> T): T? =
  try {
    call()
  } catch (e: CancellationException) {
    throw e
  } catch (_: Exception) {
    null
  }
