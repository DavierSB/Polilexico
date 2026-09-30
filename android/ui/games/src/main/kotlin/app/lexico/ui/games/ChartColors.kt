package app.lexico.ui.games

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp

internal val DOT_RADIUS = 5.dp
internal val DOT_RING = 2.dp
internal val TAP_RADIUS = 18.dp
internal val LEFT_MARGIN = 34.dp
internal val BOTTOM_MARGIN = 22.dp

internal class ChartColors(val dot: Color, val background: Color, val grid: Color, val faint: Color, val axisText: TextStyle)

@Composable
internal fun chartColors(): ChartColors = ChartColors(
  dot = MaterialTheme.colorScheme.primary, background = MaterialTheme.colorScheme.surface,
  grid = MaterialTheme.colorScheme.outlineVariant, faint = MaterialTheme.colorScheme.onSurfaceVariant,
  axisText = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
)
