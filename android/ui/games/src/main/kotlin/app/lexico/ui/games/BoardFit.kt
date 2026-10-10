package app.lexico.ui.games

import androidx.compose.runtime.Composable
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val MIN_BOARD = 260.dp
private val GAP = 4.dp

@Composable
internal fun BoardFit(height: Dp, top: @Composable () -> Unit, board: @Composable () -> Unit, bottom: @Composable () -> Unit) {
  Layout(contents = listOf(top, board, bottom)) { (above, square, below), constraints ->
    val width = constraints.maxWidth
    val free = Constraints(maxWidth = width)
    val upper = measureAll(above, free)
    val lower = measureAll(below, free)
    val gap = GAP.roundToPx()
    val rest = height.roundToPx() - upper.sumOf { it.height } - lower.sumOf { it.height } - 2 * gap
    val side = rest.coerceIn(minOf(MIN_BOARD.roundToPx(), width), width)
    val boards = measureAll(square, Constraints.fixed(side, side))
    val total = upper.sumOf { it.height } + side + lower.sumOf { it.height } + 2 * gap
    layout(width, total) {
      var y = 0
      upper.forEach { it.place(0, y); y += it.height }
      y += gap
      boards.forEach { it.place((width - side) / 2, y) }
      y += side + gap
      lower.forEach { it.place(0, y); y += it.height }
    }
  }
}

private fun measureAll(items: List<Measurable>, constraints: Constraints): List<Placeable> = items.map { it.measure(constraints) }
