package app.lexico.ui.board

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import app.lexico.model.Position

/**
 * Una ficha que se arrastra con el dedo, del atril o del tablero del [TilePlacer]. El tablero y el
 * atril dicen donde estan en la ventana, y asi se sabe sobre que casilla o hueco se suelta.
 */
@Stable
class TileDrag {
  /** La ficha arrastrada (indice en el atril); `null` = no se arrastra nada. */
  var tile: Int? by mutableStateOf(null)
    private set

  /** La casilla de donde salio, si es una ficha puesta en el tablero. */
  var from: Position? by mutableStateOf(null)
    private set

  /** Donde se soltaria: el centro de la ficha flotante, en coordenadas de la ventana. */
  var point: Offset by mutableStateOf(Offset.Zero)
    private set

  internal var board: LayoutCoordinates? = null
  internal var boardMargin = false
  internal var rack: LayoutCoordinates? = null
  internal val rackTiles = mutableMapOf<Int, LayoutCoordinates>()

  internal fun start(tile: Int, from: Position?, point: Offset) {
    this.tile = tile
    this.from = from
    this.point = point
  }

  internal fun moveBy(delta: Offset) {
    point += delta
  }

  internal fun end() {
    tile = null
    from = null
  }

  /** La casilla bajo [point], o `null` si no cae en el tablero. */
  internal fun square(): Position? = boardGeometry()?.positionAt(board!!.windowToLocal(point))

  /** El lado de una casilla, en pixeles; `null` si no hay tablero. */
  internal fun squareSide(): Float? = boardGeometry()?.side

  /** El hueco del atril bajo [point] (cuantas fichas quedan a su izquierda), o `null` si no esta encima. */
  internal fun slot(order: List<Int>): Int? {
    val bounds = rack?.takeIf { it.isAttached }?.boundsInWindow() ?: return null
    if (point.y !in bounds.top - bounds.height..bounds.bottom + bounds.height) return null
    return order.count { it != tile && centerX(it)?.let { x -> x < point.x } == true }
  }

  /** [point] en las coordenadas del atril (donde se dibuja la ficha flotante). */
  internal fun localPoint(): Offset = rack?.takeIf { it.isAttached }?.windowToLocal(point) ?: Offset.Zero

  private fun boardGeometry(): BoardGeometry? =
    board?.takeIf { it.isAttached }?.let { BoardGeometry(it.size.width.toFloat(), boardMargin) }

  private fun centerX(i: Int): Float? = rackTiles[i]?.takeIf { it.isAttached }?.boundsInWindow()?.center?.x
}
