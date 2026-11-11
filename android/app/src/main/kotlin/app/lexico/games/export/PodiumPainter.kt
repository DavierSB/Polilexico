package app.lexico.games.export

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.sp
import app.lexico.ui.board.BoardStyle

internal const val PODIUM_HEIGHT = 200

private const val MARGIN = 24f
private const val ROW = 40f
private const val TOP = 12f
private const val GAP = 16f
private const val MEDAL = 14f
private const val NAME_PX = 22f
private const val VALUE_PX = 20f
private const val MASTER_PX = 20f
private val MEDALS = listOf(Color(0xFFD4AF37), Color(0xFFC0C0C0), Color(0xFFCD7F32))
private val MEDAL_TEXT = Color(0xFF2B2B2B)

internal class LineWriter(private val measurer: TextMeasurer, private val style: BoardStyle) {
  fun line(text: String, px: Float, weight: FontWeight, maxWidth: Float, color: Color = style.bonusText): TextLayoutResult = measurer.measure(
    text, TextStyle(color = color, fontSize = px.sp, fontWeight = weight, fontFamily = style.fontFamily),
    overflow = TextOverflow.Ellipsis, maxLines = 1, constraints = Constraints(maxWidth = maxWidth.toInt().coerceAtLeast(1)),
  )
}

internal fun DrawScope.drawPodium(podium: Podium, writer: LineWriter, line: Color) {
  podium.places.forEachIndexed { i, place -> drawPlace(i, place, TOP + ROW * i + ROW / 2, writer) }
  val masterY = TOP + ROW * MEDALS.size + GAP + ROW / 2
  drawLine(line.copy(alpha = 0.35f), Offset(MARGIN, masterY - ROW / 2 - GAP / 2), Offset(size.width - MARGIN, masterY - ROW / 2 - GAP / 2), 1.5f)
  drawRowText("Máster", "${podium.master}", masterY, MARGIN + MEDAL * 2 + 12f, writer, MASTER_PX)
}

private fun DrawScope.drawPlace(rank: Int, place: Place, centerY: Float, writer: LineWriter) {
  drawMedal(rank, centerY, writer)
  drawRowText(place.name, "${place.total} · ${place.percent} %", centerY, MARGIN + MEDAL * 2 + 12f, writer, NAME_PX)
}

private fun DrawScope.drawMedal(rank: Int, centerY: Float, writer: LineWriter) {
  val center = Offset(MARGIN + MEDAL, centerY)
  drawCircle(MEDALS[rank], MEDAL, center)
  val number = writer.line("${rank + 1}", 16f, FontWeight.Bold, MEDAL * 2, MEDAL_TEXT)
  drawText(number, topLeft = Offset(center.x - number.size.width / 2f, centerY - number.size.height / 2f))
}

private fun DrawScope.drawRowText(name: String, value: String, centerY: Float, left: Float, writer: LineWriter, px: Float) {
  val shown = writer.line(value, VALUE_PX, FontWeight.Bold, size.width / 2)
  val right = size.width - MARGIN
  drawText(shown, topLeft = Offset(right - shown.size.width, centerY - shown.size.height / 2f))
  val label = writer.line(name, px, FontWeight.Bold, right - shown.size.width - 16f - left)
  drawText(label, topLeft = Offset(left, centerY - label.size.height / 2f))
}
