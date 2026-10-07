package app.lexico.sound

import android.app.NotificationManager
import android.content.Context
import android.media.AudioManager

internal class PhoneProfile(ctx: Context) {
  private val audio = ctx.getSystemService(AudioManager::class.java)
  private val notifications = ctx.getSystemService(NotificationManager::class.java)

  fun allowsSound(): Boolean = audio.ringerMode == AudioManager.RINGER_MODE_NORMAL && !doNotDisturb()

  fun allowsVibration(): Boolean = audio.ringerMode != AudioManager.RINGER_MODE_SILENT && !doNotDisturb()

  private fun doNotDisturb(): Boolean = notifications.currentInterruptionFilter !in QUIET_FILTERS

  private companion object {
    val QUIET_FILTERS = setOf(NotificationManager.INTERRUPTION_FILTER_ALL, NotificationManager.INTERRUPTION_FILTER_UNKNOWN)
  }
}
