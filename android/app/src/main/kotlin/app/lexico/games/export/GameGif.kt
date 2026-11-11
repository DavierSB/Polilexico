package app.lexico.games.export

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import app.lexico.game.records.GameReview
import app.lexico.ui.board.BoardStyle
import app.lexico.ui.board.drawStillBoard
import java.io.OutputStream

internal class GameGif(context: Context, private val style: BoardStyle) {
  private val density = Density(1f)
  private val measurer = TextMeasurer(createFontFamilyResolver(context), density, LayoutDirection.Ltr)
  private val logo = logoImage(context, LOGO_WIDTH, LOGO_HEIGHT)
  private val writer = LineWriter(measurer, style)

  fun write(review: GameReview, out: OutputStream) = write(stills(review), out)

  fun write(stills: List<Still>, out: OutputStream) {
    val frames = stills.mapIndexed { i, still -> GifFrame(pixels(still), if (i == stills.lastIndex) LAST_DELAY_CS else DELAY_CS) }
    GifEncoder(WIDTH, heightOf(stills.first())).write(frames, out)
  }

  fun writeJpeg(review: GameReview, out: OutputStream) = writeJpeg(stills(review).last(), out)

  fun writeJpeg(still: Still, out: OutputStream) {
    image(still).asAndroidBitmap().compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
  }

  private fun heightOf(still: Still): Int = if (still.podium == null) HEIGHT else HEIGHT + PODIUM_HEIGHT

  private fun pixels(still: Still): IntArray {
    val height = heightOf(still)
    return IntArray(WIDTH * height).also { image(still).asAndroidBitmap().getPixels(it, 0, WIDTH, 0, 0, WIDTH, height) }
  }

  private fun image(still: Still): ImageBitmap {
    val height = heightOf(still)
    val image = ImageBitmap(WIDTH, height)
    CanvasDrawScope().draw(density, LayoutDirection.Ltr, Canvas(image), Size(WIDTH.toFloat(), height.toFloat())) { drawStill(still) }
    return image
  }

  private fun DrawScope.drawStill(still: Still) {
    still.podium?.let { return drawTableStill(still, it) }
    drawRect(style.backdrop)
    drawImage(logo, Offset(LOGO_MARGIN, (CAPTION - LOGO_HEIGHT) / 2f))
    drawCaption(still.score, CAPTION * 0.32f, SCORE_PX, FontWeight.Bold)
    drawCaption(still.caption, CAPTION * 0.74f, CAPTION_PX, FontWeight.Normal)
    inset(0f, CAPTION.toFloat(), 0f, 0f) { drawStillBoard(still.board, style, measurer) }
  }

  private fun DrawScope.drawTableStill(still: Still, podium: Podium) {
    drawRect(style.backdrop)
    drawImage(logo, Offset(LOGO_MARGIN, (CAPTION - LOGO_HEIGHT) / 2f))
    drawHeading(still.score, CAPTION * 0.32f, SCORE_PX, FontWeight.Bold)
    drawHeading(still.caption, CAPTION * 0.72f, ROUND_PX, FontWeight.Normal)
    inset(0f, CAPTION.toFloat(), 0f, PODIUM_HEIGHT.toFloat()) { drawStillBoard(still.board, style, measurer) }
    inset(0f, (CAPTION + WIDTH).toFloat(), 0f, 0f) { drawPodium(podium, writer, style.bonusText) }
  }

  private fun DrawScope.drawHeading(text: String, centerY: Float, px: Float, weight: FontWeight) {
    val layout = writer.line(text, px, weight, size.width - TEXT_LEFT - LOGO_MARGIN)
    drawText(layout, topLeft = Offset(TEXT_LEFT, centerY - layout.size.height / 2))
  }

  private fun DrawScope.drawCaption(text: String, centerY: Float, px: Float, weight: FontWeight) {
    val layout = measurer.measure(text, TextStyle(color = style.bonusText, fontSize = px.sp, fontWeight = weight, fontFamily = style.fontFamily))
    drawText(layout, topLeft = Offset((size.width - layout.size.width) / 2, centerY - layout.size.height / 2))
  }

  private companion object {
    const val WIDTH = 600
    const val CAPTION = 96
    const val HEIGHT = WIDTH + CAPTION
    const val SCORE_PX = 30f
    const val CAPTION_PX = 24f
    const val DELAY_CS = 120
    const val LAST_DELAY_CS = 500
    const val JPEG_QUALITY = 92
    const val LOGO_HEIGHT = 50
    const val LOGO_WIDTH = LOGO_HEIGHT * 116 / 100
    const val LOGO_MARGIN = 16f
    const val TEXT_LEFT = LOGO_MARGIN * 2 + LOGO_WIDTH
    const val ROUND_PX = 21f
  }
}
