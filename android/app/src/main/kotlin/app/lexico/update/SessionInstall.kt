package app.lexico.update

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import java.io.File

internal fun installApk(ctx: Context, apk: File) {
  runCatching { commitSession(ctx, apk) }.onFailure { openInstaller(ctx, apk) }
}

internal fun openInstaller(ctx: Context, apk: File) {
  ctx.startActivity(installIntent(ctx, apk))
}

private fun commitSession(ctx: Context, apk: File) {
  val installer = ctx.packageManager.packageInstaller
  val id = installer.createSession(sessionParams())
  installer.openSession(id).use { session ->
    write(session, apk)
    session.commit(statusSender(ctx, id).intentSender)
  }
}

private fun sessionParams(): PackageInstaller.SessionParams =
  PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
  }

private fun write(session: PackageInstaller.Session, apk: File) {
  session.openWrite("polilexico.apk", 0, apk.length()).use { out ->
    apk.inputStream().use { it.copyTo(out) }
    session.fsync(out)
  }
}

private fun statusSender(ctx: Context, id: Int): PendingIntent {
  val intent = Intent(ctx, InstallStatusReceiver::class.java)
  return PendingIntent.getBroadcast(ctx, id, intent, PendingIntent.FLAG_UPDATE_CURRENT or mutableFlag())
}

private fun mutableFlag(): Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
