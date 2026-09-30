package app.lexico.ui.analysis

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import app.lexico.ui.common.RankedMove
import kotlinx.coroutines.CancellationException

@Composable
internal fun rememberAnalysis(): Analysis = remember { Analysis() }

@Stable
internal class Analysis {
  var result: List<RankedMove>? by mutableStateOf(null)
    private set
  var selected: RankedMove? by mutableStateOf(null)
  var message: String? by mutableStateOf(null)
    private set
  var running by mutableStateOf(false)
    private set

  suspend fun run(analyst: Analyst, editor: PositionEditor) {
    running = true
    try {
      show(analyst.bestMoves(editor.state.board, editor.rack.toList()))
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      message = e.message ?: e.toString()
    } finally {
      running = false
    }
  }

  fun clear() {
    result = null
    selected = null
    message = null
  }

  private fun show(candidates: List<RankedMove>) {
    result = candidates
    selected = candidates.firstOrNull()
    message = if (candidates.isEmpty()) "No hay jugadas posibles." else null
  }
}
