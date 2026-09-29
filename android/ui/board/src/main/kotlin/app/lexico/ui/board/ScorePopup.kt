package app.lexico.ui.board

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.drawText
import app.lexico.model.Board
import app.lexico.model.Position

/** Cuanto se ven los puntos de una jugada ("+34") antes de desvanecerse. */
private const val SCORE_MS = 1600

/** Desde estos puntos, una jugada se anima en rojo. */
private const val HIGH_SCORE = 40

/**
 * Los puntos de la ultima jugada mientras se animan: aparecen, suben y se desvanecen. Se relanza
 * cada vez que el tablero trae una jugada nueva. `null` si no hay nada que mostrar.
 */
@Composable
internal fun rememberScorePopup(board: Board, score: Int?): ScorePopup? {
  val shown = score != null && board.latest.isNotEmpty()
  val progress = remember { Animatable(1f) }
  LaunchedEffect(board.latest, shown) {
    progress.snapTo(if (shown) 0f else 1f)
    progress.animateTo(1f, tween(SCORE_MS, easing = LinearEasing))
  }
  return if (shown) ScorePopup(score!!, lastSquare(board.latest), progress) else null
}

/** La animacion en curso. `rise` va de 0 a 1 a lo largo de toda la animacion. */
@Stable
internal class ScorePopup(val score: Int, val square: Position, private val progress: Animatable<Float, AnimationVector1D>) {
  /** El color de los puntos: rojo en las jugadas grandes. */
  fun ink(style: BoardStyle): Color = if (score >= HIGH_SCORE) style.highScoreInk else style.scoreInk

  val rise: Float get() = progress.value

  val visible: Boolean get() = rise < 1f

  /** Aparece rapido, se mantiene y se desvanece al final. */
  val alpha: Float
    get() = when {
      rise < 0.12f -> rise / 0.12f
      rise > 0.65f -> (1f - rise) / 0.35f
      else -> 1f
    }
}

/** Los puntos, centrados en `center` pero sin salirse del tablero. */
internal fun DrawScope.drawScoreText(text: TextLayoutResult, center: Offset, alpha: Float) {
  val box = Size(text.size.width.toFloat(), text.size.height.toFloat())
  drawText(text, topLeft = clampedTopLeft(center, box), alpha = alpha)
}

private fun DrawScope.clampedTopLeft(center: Offset, box: Size): Offset = Offset(
  (center.x - box.width / 2).coerceIn(0f, maxOf(0f, size.width - box.width)),
  (center.y - box.height / 2).coerceIn(0f, maxOf(0f, size.height - box.height)),
)

/** La ultima ficha de la palabra: la de mas abajo a la derecha. */
internal fun lastSquare(latest: Set<Position>): Position = latest.maxWith(compareBy({ it.row }, { it.column }))
