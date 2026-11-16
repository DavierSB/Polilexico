package app.lexico.update

import android.content.Context
import androidx.core.content.edit
import java.io.File

private const val PREFS = "updates"
private const val READY_KEY = "readyVersion"
private const val TOTAL_KEY = "partTotal"

internal fun apkFile(ctx: Context): File = File(updatesDir(ctx), "polilexico.apk")

internal fun partFile(ctx: Context, version: String): File = File(updatesDir(ctx), "polilexico-$version.part")

internal fun readyApk(ctx: Context, version: String): File? =
  apkFile(ctx).takeIf { it.exists() && prefs(ctx).getString(READY_KEY, null) == version }

internal fun markReady(ctx: Context, version: String, part: File): File {
  val apk = apkFile(ctx).apply { delete() }
  part.renameTo(apk)
  prefs(ctx).edit { putString(READY_KEY, version) }
  return apk
}

internal fun discardUpdates(ctx: Context, keep: File? = null) {
  updatesDir(ctx).listFiles()?.filter { it != keep }?.forEach(File::delete)
  prefs(ctx).edit { remove(READY_KEY) }
}

internal fun saveTotal(ctx: Context, total: Long) {
  prefs(ctx).edit { putLong(TOTAL_KEY, total) }
}

internal fun partFraction(ctx: Context): Float {
  val total = prefs(ctx).getLong(TOTAL_KEY, 0L)
  val part = updatesDir(ctx).listFiles()?.firstOrNull { it.extension == "part" } ?: return 0f
  return if (total > 0) part.length().toFloat() / total else 0f
}

private fun updatesDir(ctx: Context): File = File(ctx.filesDir, "updates").apply { mkdirs() }

private fun prefs(ctx: Context) = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
