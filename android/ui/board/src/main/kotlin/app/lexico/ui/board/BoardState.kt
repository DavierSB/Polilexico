package app.lexico.ui.board

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import app.lexico.model.Board
import app.lexico.model.Placement
import app.lexico.model.Position
import app.lexico.model.Tile

@Composable
fun rememberBoardState(initial: Board = Board.EMPTY): BoardState = remember { BoardState(initial) }

@Stable
class BoardState(initial: Board = Board.EMPTY) {
  var board: Board by mutableStateOf(initial)

  var arrow: Arrow? by mutableStateOf(null)

  private val _pending = mutableStateMapOf<Position, Tile>()
  val pending: Map<Position, Tile> get() = _pending

  fun placement(): Placement? = board.placementOf(_pending)

  fun reset(fresh: Board) {
    _pending.clear()
    arrow = null
    board = fresh
  }

  fun place(p: Position, tile: Tile) {
    if (board[p] == null) _pending[p] = tile
  }

  fun remove(p: Position): Tile? = _pending.remove(p)

  fun clear() = _pending.clear()

  fun toggleArrow(p: Position) {
    val current = arrow
    arrow = when {
      current == null || current.position != p -> Arrow(p, vertical = false)
      !current.vertical -> Arrow(p, vertical = true)
      else -> null
    }
  }

  fun advanceArrow() {
    val current = arrow ?: return
    arrow = nextFree(current)?.let { current.copy(position = it) }
  }

  private fun nextFree(from: Arrow): Position? {
    var p = from.position
    do {
      p = if (from.vertical) Position(p.row + 1, p.column) else Position(p.row, p.column + 1)
    } while (p.onBoard && (board[p] != null || p in _pending))
    return p.takeIf { it.onBoard }
  }
}
