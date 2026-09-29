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

/** El resultado del analisis de la posicion: las jugadas, la elegida y si se esta calculando. */
@Stable
internal class Analysis {
  /** Las mejores jugadas; `null` mientras se edita la posicion. */
  var result: List<RankedMove>? by mutableStateOf(null)
    private set
  var selected: RankedMove? by mutableStateOf(null)
  var message: String? by mutableStateOf(null)
    private set
  var running by mutableStateOf(false)
    private set

  /** Pide al [analyst] las jugadas de la posicion del [editor]. */
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

  /** Vuelve a editar: descarta el analisis. */
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
