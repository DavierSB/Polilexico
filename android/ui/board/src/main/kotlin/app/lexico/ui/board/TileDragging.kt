package app.lexico.ui.board

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round

private val LIFT = 40.dp

private const val BOARD_SCALE = 1.3f

@Composable
internal fun DraggedTile(c: TilePlacer, style: BoardStyle, rackSize: Dp) {
  val i = c.drag.tile ?: return
  val overBoard by remember { derivedStateOf { c.drag.square() != null } }
  val side = c.drag.squareSide()?.takeIf { overBoard }
  val size = side?.let { with(LocalDensity.current) { (it * BOARD_SCALE).toDp() } } ?: rackSize
  Box(Modifier.offset { c.drag.localPoint().round() - IntOffset(size.roundToPx() / 2, size.roundToPx() / 2) }) {
    RackTile(c.tiles[i], style, size, marked = true)
  }
}

internal fun Modifier.boardDrags(c: TilePlacer, style: BoardStyle, enabled: Boolean): Modifier = this
  .onGloballyPositioned { c.drag.board = it; c.drag.boardMargin = style.showCoordinates }
  .then(if (enabled) Modifier.pointerInput(c, style.showCoordinates) { dragPlacedTiles(c, style) } else Modifier)

internal fun Modifier.rackDrops(c: TilePlacer): Modifier = onGloballyPositioned { c.drag.rack = it }

internal fun Modifier.rackTileDrags(c: TilePlacer, i: Int, enabled: Boolean): Modifier = this
  .onGloballyPositioned { c.drag.rackTiles[i] = it }
  .then(if (enabled) Modifier.pointerInput(c, i) { dragRackTile(c, i) } else Modifier)

private suspend fun PointerInputScope.dragRackTile(c: TilePlacer, i: Int) = detectDragGestures(
  onDragStart = { at -> c.drag.rackTiles[i]?.let { c.drag.start(i, null, it.localToWindow(at) - lift()) } },
  onDragEnd = c::drop,
  onDragCancel = c.drag::end,
) { change, amount ->
  change.consume()
  c.drag.moveBy(amount)
}

private suspend fun PointerInputScope.dragPlacedTiles(c: TilePlacer, style: BoardStyle) = awaitEachGesture {
  val down = awaitFirstDown(requireUnconsumed = false)
  val from = BoardGeometry(size.width.toFloat(), style.showCoordinates).positionAt(down.position)
  val i = from?.let { c.placed[it] } ?: return@awaitEachGesture
  val slop = awaitTouchSlopOrCancellation(down.id) { change, _ -> change.consume() } ?: return@awaitEachGesture
  val board = c.drag.board ?: return@awaitEachGesture
  c.drag.start(i, from, board.localToWindow(slop.position) - lift())
  val finished = drag(slop.id) { c.drag.moveBy(it.positionChange()); it.consume() }
  if (finished) c.drop() else c.drag.end()
}

private fun PointerInputScope.lift(): Offset = Offset(0f, LIFT.toPx())
