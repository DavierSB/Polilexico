package app.lexico.sound

import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import app.lexico.game.Cue

internal class CueVibrations(ctx: Context) {
  private val vibrator = vibratorOf(ctx)

  fun play(cue: Cue) {
    if (!vibrator.hasVibrator()) return
    val timings = patternOf(cue)
    when {
      Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> vibrator.vibrate(waveform(timings), touchUsage())
      Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> vibrator.vibrate(waveform(timings), sonificationUsage())
      else -> @Suppress("DEPRECATION") vibrator.vibrate(timings, -1)
    }
  }
}

private fun vibratorOf(ctx: Context): Vibrator =
  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) ctx.getSystemService(VibratorManager::class.java).defaultVibrator
  else ctx.getSystemService(Vibrator::class.java)

private fun waveform(timings: LongArray): VibrationEffect = VibrationEffect.createWaveform(timings, -1)

private fun touchUsage(): VibrationAttributes = VibrationAttributes.createForUsage(VibrationAttributes.USAGE_TOUCH)

private fun sonificationUsage(): AudioAttributes =
  AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION).build()

private fun patternOf(cue: Cue): LongArray = when (cue) {
  Cue.WARNING -> longArrayOf(0, 80)
  Cue.LAST_SECONDS -> longArrayOf(0, 60, 80, 60, 80, 60)
  Cue.TIME_UP -> longArrayOf(0, 400)
  Cue.CORRECT -> longArrayOf(0, 40)
  Cue.WRONG -> longArrayOf(0, 60, 60, 60)
  Cue.MISS -> longArrayOf(0, 120)
  Cue.OPPONENT -> longArrayOf(0, 30)
  Cue.CELEBRATION -> longArrayOf(0, 60, 40, 60, 40, 60, 80, 300)
  Cue.GAME_OVER -> longArrayOf(0, 60, 60, 60, 60, 200)
}
