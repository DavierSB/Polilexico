package app.lexico.games.export

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import app.lexico.R

internal fun logoImage(context: Context, width: Int, height: Int): ImageBitmap {
  val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
  val shell = requireNotNull(context.getDrawable(R.drawable.logo_shell))
  shell.setBounds(0, 0, width, height)
  shell.draw(Canvas(bitmap))
  return bitmap.asImageBitmap()
}
