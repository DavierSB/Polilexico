package app.lexico.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp

private val GAP = 4.dp

private const val MAX_FONT_SCALE = 1f

@Composable
fun BoardColumn(
  top: @Composable ColumnScope.() -> Unit,
  board: @Composable () -> Unit,
  bottom: @Composable ColumnScope.() -> Unit,
  footer: @Composable ColumnScope.() -> Unit = {},
) {
  val scale = remember { mutableFloatStateOf(1f) }
  CompositionLocalProvider(LocalDensity provides scaled(LocalDensity.current, scale.floatValue, MAX_FONT_SCALE)) {
    Layout(listOf({ Stack(top) }, { Box { board() } }, { Stack(bottom) }, { Stack(footer) }), Modifier.fillMaxSize()) { slots, constraints ->
      val loose = Constraints(maxWidth = constraints.maxWidth)
      val (t, b, m, f) = slots.map { it.first().measure(loose) }
      val gap = GAP.roundToPx()
      fit(scale, constraints.maxHeight - b.height, t.height + m.height + f.height + gap * 3)
      layout(constraints.maxWidth, constraints.maxHeight) { stack(listOf(t, b, m), gap, f, constraints.maxHeight) }
    }
  }
}

@Composable
private fun Stack(content: @Composable ColumnScope.() -> Unit) {
  Column(verticalArrangement = Arrangement.spacedBy(GAP), content = content)
}

private fun Placeable.PlacementScope.stack(parts: List<Placeable>, gap: Int, footer: Placeable, height: Int) {
  var y = 0
  parts.forEach { it.place(0, y); y += it.height + gap }
  footer.place(0, maxOf(y, height - footer.height))
}
