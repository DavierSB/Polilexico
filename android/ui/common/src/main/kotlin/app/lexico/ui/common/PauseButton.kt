package app.lexico.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp

/** El boton de pausa: las dos barras verticales de siempre. */
@Composable
fun PauseButton(onClick: () -> Unit) {
  val ink = MaterialTheme.colorScheme.primary
  Box(Modifier.size(40.dp).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
    Canvas(Modifier.size(18.dp)) { drawPauseBars(ink) }
  }
}

/** Dos barras redondeadas, cada una de un tercio del ancho, con un tercio de hueco entre ellas. */
private fun DrawScope.drawPauseBars(ink: Color) {
  val bar = Size(size.width / 3, size.height)
  val radius = CornerRadius(bar.width / 3)
  drawRoundRect(ink, Offset.Zero, bar, radius)
  drawRoundRect(ink, Offset(size.width - bar.width, 0f), bar, radius)
}
