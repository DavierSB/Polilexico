package app.lexico.games.export

import android.content.Context
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

  fun write(review: GameReview, out: OutputStream) {
    val stills = stills(review)
    val frames = stills.mapIndexed { i, still -> GifFrame(pixels(still), if (i == stills.lastIndex) LAST_DELAY_CS else DELAY_CS) }
    GifEncoder(WIDTH, HEIGHT).write(frames, out)
  }

  private fun pixels(still: Still): IntArray {
    val image = ImageBitmap(WIDTH, HEIGHT)
    CanvasDrawScope().draw(density, LayoutDirection.Ltr, Canvas(image), Size(WIDTH.toFloat(), HEIGHT.toFloat())) { drawStill(still) }
    return IntArray(WIDTH * HEIGHT).also { image.asAndroidBitmap().getPixels(it, 0, WIDTH, 0, 0, WIDTH, HEIGHT) }
  }

  private fun DrawScope.drawStill(still: Still) {
    drawRect(style.backdrop)
    drawCaption(still.score, CAPTION * 0.32f, SCORE_PX, FontWeight.Bold)
    drawCaption(still.caption, CAPTION * 0.74f, CAPTION_PX, FontWeight.Normal)
    inset(0f, CAPTION.toFloat(), 0f, 0f) { drawStillBoard(still.board, style, measurer) }
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
  }
}
