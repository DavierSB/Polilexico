package app.lexico.soundboard

import android.content.Context
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import app.lexico.ui.common.CONFETTI_MS
import kotlinx.coroutines.delay

private const val FADE_MS = 500L
private const val FADE_STEPS = 10
private const val REPEAT_GAP_MS = 150L

private val main = Handler(Looper.getMainLooper())

fun play(ctx: Context, variant: Variant) {
  val res = rawSound(ctx, variant.file) ?: return
  val player = MediaPlayer.create(ctx, res) ?: return
  player.playbackParams = player.playbackParams.setPitch(variant.pitch)
  player.pause()
  player.seekTo(0)
  playTimes(player, variant.times)
  variant.maxMs?.let { main.postDelayed({ runCatching { player.stop(); player.release() } }, it) }
}

private fun playTimes(player: MediaPlayer, times: Int) {
  player.setOnCompletionListener {
    if (times > 1) main.postDelayed({ it.seekTo(0); playTimes(it, times - 1) }, REPEAT_GAP_MS) else it.release()
  }
  player.start()
}

suspend fun celebrate(ctx: Context, res: Int) {
  val player = MediaPlayer.create(ctx, res) ?: return
  try {
    player.start()
    delay(CONFETTI_MS - FADE_MS)
    fadeOut(player)
  } finally {
    player.release()
  }
}

private suspend fun fadeOut(player: MediaPlayer) {
  for (step in FADE_STEPS - 1 downTo 0) {
    val volume = step.toFloat() / FADE_STEPS
    player.setVolume(volume, volume)
    delay(FADE_MS / FADE_STEPS)
  }
}
