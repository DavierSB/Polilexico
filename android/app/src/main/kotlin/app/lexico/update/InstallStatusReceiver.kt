package app.lexico.update

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import androidx.core.content.IntentCompat

class InstallStatusReceiver : BroadcastReceiver() {
  override fun onReceive(ctx: Context, intent: Intent) {
    when (intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)) {
      PackageInstaller.STATUS_PENDING_USER_ACTION -> confirm(ctx, intent)
      PackageInstaller.STATUS_SUCCESS, PackageInstaller.STATUS_FAILURE_ABORTED -> Unit
      else -> openInstaller(ctx, apkFile(ctx))
    }
  }

  private fun confirm(ctx: Context, intent: Intent) {
    val prompt = IntentCompat.getParcelableExtra(intent, Intent.EXTRA_INTENT, Intent::class.java) ?: return
    ctx.startActivity(prompt.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
  }
}
