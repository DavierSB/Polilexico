package app.lexico.ui.common

import androidx.compose.foundation.ScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp

@Composable
fun Modifier.scrollbar(state: ScrollState): Modifier {
  val color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
  return drawWithContent {
    drawContent()
    val total = size.height + state.maxValue
    if (state.maxValue <= 0 || total <= 0f) return@drawWithContent
    val width = 4.dp.toPx()
    val thumb = (size.height * size.height / total).coerceAtLeast(24.dp.toPx())
    val top = (size.height - thumb) * state.value / state.maxValue
    drawRoundRect(color, Offset(size.width - width, top), Size(width, thumb), CornerRadius(width / 2))
  }
}
