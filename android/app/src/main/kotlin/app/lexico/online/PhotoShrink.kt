package app.lexico.online

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

private const val PHOTO_SIDE = 128
private const val PHOTO_QUALITY = 80

suspend fun shrunkPhoto(context: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
  runCatching { square(decode(context, uri)) }.getOrNull()?.let(::jpegBase64)
}

private fun decode(context: Context, uri: Uri): Bitmap =
  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) decodeModern(context, uri) else decodeLegacy(context, uri)

private fun decodeModern(context: Context, uri: Uri): Bitmap =
  ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { decoder, info, _ ->
    val shortSide = minOf(info.size.width, info.size.height)
    val factor = maxOf(1, shortSide / (PHOTO_SIDE * 2))
    decoder.setTargetSize(info.size.width / factor, info.size.height / factor)
    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
  }

private fun decodeLegacy(context: Context, uri: Uri): Bitmap {
  val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
  context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
  val shortSide = minOf(bounds.outWidth, bounds.outHeight)
  val options = BitmapFactory.Options().apply { inSampleSize = maxOf(1, shortSide / (PHOTO_SIDE * 2)) }
  return context.contentResolver.openInputStream(uri)!!.use { BitmapFactory.decodeStream(it, null, options)!! }
}

private fun square(bitmap: Bitmap): Bitmap {
  val side = minOf(bitmap.width, bitmap.height)
  val cropped = Bitmap.createBitmap(bitmap, (bitmap.width - side) / 2, (bitmap.height - side) / 2, side, side)
  return Bitmap.createScaledBitmap(cropped, PHOTO_SIDE, PHOTO_SIDE, true)
}

private fun jpegBase64(bitmap: Bitmap): String {
  val out = ByteArrayOutputStream()
  bitmap.compress(Bitmap.CompressFormat.JPEG, PHOTO_QUALITY, out)
  return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
}
