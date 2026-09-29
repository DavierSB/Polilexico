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

/**
 * Tablero editable: el [board] recibido, mas las fichas [pending] que el usuario va
 * poniendo antes de confirmar, y la [arrow] de escritura. Nada de esto llega al motor hasta
 * que se pide la [placement].
 */
@Stable
class BoardState(initial: Board = Board.EMPTY) {
  var board: Board by mutableStateOf(initial)

  /** Flecha de escritura; `null` = sin flecha. */
  var arrow: Arrow? by mutableStateOf(null)

  private val _pending = mutableStateMapOf<Position, Tile>()
  val pending: Map<Position, Tile> get() = _pending

  /** Las provisionales como jugada ("H8 CA.A"), o `null` si no forman una linea continua. */
  fun placement(): Placement? = board.placementOf(_pending)

  /** Nuevo tablero de partida: se descartan provisionales y flecha. */
  fun reset(fresh: Board) {
    _pending.clear()
    arrow = null
    board = fresh
  }

  /** Pone una ficha provisional (solo en casillas vacias). */
  fun place(p: Position, tile: Tile) {
    if (board[p] == null) _pending[p] = tile
  }

  /** Retira la provisional de `p` y la devuelve, o `null` si no habia. */
  fun remove(p: Position): Tile? = _pending.remove(p)

  /** Retira todas las provisionales. */
  fun clear() = _pending.clear()

  /**
   * Toque en una casilla vacia para la flecha, como en ISC: el primero la pone horizontal, el
   * segundo en la misma casilla la pone vertical y el tercero la quita.
   */
  fun toggleArrow(p: Position) {
    val current = arrow
    arrow = when {
      current == null || current.position != p -> Arrow(p, vertical = false)
      !current.vertical -> Arrow(p, vertical = true)
      else -> null
    }
  }

  /** Pasa la flecha a la siguiente casilla libre en su direccion (o la quita al salirse). */
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
