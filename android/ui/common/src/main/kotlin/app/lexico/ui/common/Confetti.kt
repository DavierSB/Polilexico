package app.lexico.ui.common

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import kotlin.math.sin
import kotlin.random.Random

private val ConfettiColors = listOf(
  Color(0xFF66BB6A), Color(0xFFFFCA28), Color(0xFF42A5F5), Color(0xFFEC407A), Color(0xFFFF7043), Color(0xFFAB47BC),
)

private const val PIECES = 110
const val CONFETTI_MS = 3600

private class Piece(
  val x: Float, val delay: Float, val speed: Float, val sway: Float, val phase: Float, val spin: Float,
  val width: Float, val color: Color,
)

val LocalConfettiStart = staticCompositionLocalOf<() -> Unit> { {} }

@Composable
fun Confetti(modifier: Modifier = Modifier) {
  val pieces = remember { List(PIECES) { randomPiece(Random) } }
  val time = remember { Animatable(0f) }
  val onStart = LocalConfettiStart.current
  LaunchedEffect(Unit) {
    onStart()
    time.animateTo(1f, tween(CONFETTI_MS, easing = LinearEasing))
  }
  Canvas(modifier) { pieces.forEach { drawPiece(it, time.value) } }
}

private fun randomPiece(r: Random): Piece = Piece(
  x = r.nextFloat(), delay = r.nextFloat() * 0.45f, speed = 0.9f + r.nextFloat() * 0.7f,
  sway = 6f + r.nextFloat() * 14f, phase = r.nextFloat() * 6.3f, spin = 200f + r.nextFloat() * 700f,
  width = 6f + r.nextFloat() * 6f, color = ConfettiColors[r.nextInt(ConfettiColors.size)],
)

private fun DrawScope.drawPiece(p: Piece, t: Float) {
  val w = p.width.dp.toPx()
  val y = size.height * ((t - p.delay) * p.speed * 1.4f) - w * 2
  if (y < -w * 2 || y > size.height) return
  val x = size.width * p.x + sin(t * 9f + p.phase) * p.sway.dp.toPx()
  rotate(t * p.spin, Offset(x, y)) {
    drawRect(p.color.copy(alpha = fadeOut(t)), Offset(x - w / 2, y - w / 4), Size(w, w / 2))
  }
}

private fun fadeOut(t: Float): Float = if (t < 0.8f) 1f else (1f - t) / 0.2f
