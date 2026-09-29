package app.lexico.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Icono de bolsita como en ISC: la silueta de un saco atado, con el numero de fichas dentro. */
@Composable
fun BagIcon(tiles: Int, onClick: () -> Unit) {
  val ink = MaterialTheme.colorScheme.onSurface
  Box(Modifier.size(width = 48.dp, height = 44.dp).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
    Canvas(Modifier.size(42.dp)) { drawSack(ink) }
    Text("$tiles", Modifier.padding(top = 14.dp), color = ink, fontWeight = FontWeight.Bold, fontSize = 13.sp)
  }
}

/** El saco: cuerpo redondeado, boca fruncida y el cordel del nudo. */
private fun DrawScope.drawSack(ink: Color) {
  val stroke = Stroke(width = 1.6.dp.toPx(), join = StrokeJoin.Round, cap = StrokeCap.Round)
  drawPath(sackBody(), ink, style = stroke)
  drawPath(sackMouth(), ink, style = stroke)
  drawLine(ink, point(0.36f, 0.25f), point(0.64f, 0.25f), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
}

/** Del cuello (atado) se abre en un saco redondeado. */
private fun DrawScope.sackBody(): Path = Path().apply {
  moveTo(size.width * 0.40f, size.height * 0.26f)
  cubicTo(size.width * 0.08f, size.height * 0.36f, size.width * 0.02f, size.height * 0.96f, size.width * 0.50f, size.height * 0.96f)
  cubicTo(size.width * 0.98f, size.height * 0.96f, size.width * 0.92f, size.height * 0.36f, size.width * 0.60f, size.height * 0.26f)
}

/** La boca fruncida por encima del nudo. */
private fun DrawScope.sackMouth(): Path = Path().apply {
  moveTo(size.width * 0.40f, size.height * 0.24f)
  lineTo(size.width * 0.28f, size.height * 0.06f)
  quadraticTo(size.width * 0.39f, size.height * 0.12f, size.width * 0.50f, size.height * 0.05f)
  quadraticTo(size.width * 0.61f, size.height * 0.12f, size.width * 0.72f, size.height * 0.06f)
  lineTo(size.width * 0.60f, size.height * 0.24f)
}

/** Un punto en fracciones del ancho y el alto. */
private fun DrawScope.point(x: Float, y: Float): Offset = Offset(size.width * x, size.height * y)
