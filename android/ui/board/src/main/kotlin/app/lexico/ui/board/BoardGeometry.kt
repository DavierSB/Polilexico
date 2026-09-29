package app.lexico.ui.board

import androidx.compose.ui.geometry.Offset
import app.lexico.model.Board
import app.lexico.model.Position

/**
 * Las medidas del tablero dibujado en un ancho dado: el lado de una casilla, el margen de las
 * coordenadas y la conversion entre casillas y puntos de la pantalla.
 */
internal class BoardGeometry(width: Float, showCoordinates: Boolean) {
  private val margin = if (showCoordinates) MARGIN else 0f

  /** Lado de una casilla. */
  val side: Float = width / (Board.SIZE + margin)

  /** Esquina superior izquierda de la casilla A1. */
  val origin: Offset = Offset(side * margin, side * margin)

  /** Esquina superior izquierda de la casilla `p`. */
  fun corner(p: Position): Offset = origin + Offset(p.column * side, p.row * side)

  /** La casilla bajo un punto de la pantalla; `null` si cae en el margen o fuera. */
  fun positionAt(point: Offset): Position? {
    if (point.x < origin.x || point.y < origin.y) return null
    val p = Position(((point.y - origin.y) / side).toInt(), ((point.x - origin.x) / side).toInt())
    return p.takeIf { it.onBoard }
  }

  private companion object {
    /** Margen para las coordenadas, en casillas. */
    const val MARGIN = 0.6f
  }
}
