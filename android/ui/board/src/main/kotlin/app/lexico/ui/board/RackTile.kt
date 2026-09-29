package app.lexico.ui.board

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.lexico.model.Letters

/** Por debajo de este tamano no cabe el valor de la ficha. */
private val MIN_SIZE_WITH_VALUE = 30.dp

/** Una ficha suelta con los colores del estilo del tablero. `?` = comodin (ficha en blanco). */
@Composable
fun RackTile(letter: String, style: BoardStyle, size: Dp, marked: Boolean = false) {
  val shape = RoundedCornerShape(size * style.rounding)
  Box(Modifier.size(size).tileFace(style, shape, marked), contentAlignment = Alignment.Center) {
    if (letter != Letters.BLANK) TileLabel(letter, style, size)
  }
}

/** Una ficha boca abajo (el atril que no se ve). */
@Composable
fun FaceDownTile(style: BoardStyle, size: Dp) {
  val shape = RoundedCornerShape(size * style.rounding)
  val backColor = if (style.tileBorder != Color.Transparent) style.tileBorder else style.tripleLetter
  Box(Modifier.size(size).background(backColor, shape).border(1.dp, style.lines, shape))
}

/** Fondo y bordes: el color de ficha (o de provisional si esta marcada) y un borde resaltado. */
@Composable
private fun Modifier.tileFace(style: BoardStyle, shape: Shape, marked: Boolean): Modifier = this
  .background(if (marked) style.pending else style.tile, shape)
  .then(if (style.tileBorder != Color.Transparent) Modifier.border(1.dp, style.tileBorder, shape) else Modifier)
  .then(if (marked) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, shape) else Modifier)

/** La letra en el centro y, si cabe, su valor abajo a la derecha. */
@Composable
private fun BoxScope.TileLabel(letter: String, style: BoardStyle, size: Dp) {
  val px = size.value
  Text(letter, color = style.letter, fontWeight = style.fontWeight, fontFamily = style.fontFamily,
    fontSize = (px * if (letter.length > 1) 0.36f else 0.55f).sp)
  if (size < MIN_SIZE_WITH_VALUE) return
  Text("${Letters.value(letter)}", color = style.value, fontSize = (px * 0.22f).sp,
    modifier = Modifier.align(Alignment.BottomEnd).padding(end = 3.dp, bottom = 1.dp))
}
