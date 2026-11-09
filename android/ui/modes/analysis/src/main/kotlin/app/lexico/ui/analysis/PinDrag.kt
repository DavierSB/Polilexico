package app.lexico.ui.analysis

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import app.lexico.ui.board.BoardStyle

private val LIFT = 40.dp

internal class PinDrag(private val onDrop: (slot: Int, tile: Int) -> Unit) {
  var tile by mutableStateOf<Int?>(null)
    private set
  private var point by mutableStateOf(Offset.Zero)
  var root: LayoutCoordinates? = null
  val slots = mutableMapOf<Int, LayoutCoordinates>()
  val tiles = mutableMapOf<Int, LayoutCoordinates>()

  fun start(i: Int, at: Offset) {
    tile = i
    point = at
  }

  fun moveBy(amount: Offset) {
    point += amount
  }

  fun end() {
    tile = null
  }

  fun drop() {
    val i = tile
    val slot = slotUnder()
    end()
    if (i != null && slot != null) onDrop(slot, i)
  }

  fun localPoint(): Offset = root?.takeIf { it.isAttached }?.windowToLocal(point) ?: point

  private fun slotUnder(): Int? =
    slots.entries.firstOrNull { (_, c) -> c.isAttached && c.boundsInWindow().contains(point) }?.key
}

@Composable
internal fun DraggedPin(d: PinDrag, tiles: List<String>, style: BoardStyle) {
  val i = d.tile?.takeIf { it < tiles.size } ?: return
  Box(Modifier.offset { d.localPoint().round() - IntOffset(TILE.roundToPx() / 2, TILE.roundToPx() / 2) }) {
    LetterTile(tiles[i], style, marked = true)
  }
}

internal fun Modifier.pinSlot(d: PinDrag, slot: Int): Modifier = onGloballyPositioned { d.slots[slot] = it }

internal fun Modifier.pinSource(d: PinDrag, i: Int): Modifier = this
  .onGloballyPositioned { d.tiles[i] = it }
  .pointerInput(d, i) { dragTile(d, i) }

private suspend fun PointerInputScope.dragTile(d: PinDrag, i: Int) = detectDragGestures(
  onDragStart = { at -> d.tiles[i]?.let { d.start(i, it.localToWindow(at) - lift()) } },
  onDragEnd = d::drop,
  onDragCancel = d::end,
) { change, amount ->
  change.consume()
  d.moveBy(amount)
}

private fun PointerInputScope.lift(): Offset = Offset(0f, LIFT.toPx())
