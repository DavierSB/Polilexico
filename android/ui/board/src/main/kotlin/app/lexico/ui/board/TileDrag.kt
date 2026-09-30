package app.lexico.ui.board

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import app.lexico.model.Position

@Stable
class TileDrag {
  var tile: Int? by mutableStateOf(null)
    private set

  var from: Position? by mutableStateOf(null)
    private set

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

  internal fun square(): Position? = boardGeometry()?.positionAt(board!!.windowToLocal(point))

  internal fun squareSide(): Float? = boardGeometry()?.side

  internal fun slot(order: List<Int>): Int? {
    val bounds = rack?.takeIf { it.isAttached }?.boundsInWindow() ?: return null
    if (point.y !in bounds.top - bounds.height..bounds.bottom + bounds.height) return null
    return order.count { it != tile && centerX(it)?.let { x -> x < point.x } == true }
  }

  internal fun localPoint(): Offset = rack?.takeIf { it.isAttached }?.windowToLocal(point) ?: Offset.Zero

  private fun boardGeometry(): BoardGeometry? =
    board?.takeIf { it.isAttached }?.let { BoardGeometry(it.size.width.toFloat(), boardMargin) }

  private fun centerX(i: Int): Float? = rackTiles[i]?.takeIf { it.isAttached }?.boundsInWindow()?.center?.x
}
