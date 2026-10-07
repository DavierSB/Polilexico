package app.lexico.sounds

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer

class SoundPlayer(private val ctx: Context) {
  private val audio = AudioAttributes.Builder()
    .setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()
  private val session = ctx.getSystemService(AudioManager::class.java).generateAudioSessionId()

  fun play(sound: Sound) = play(sound.res, sound.pitch)

  fun play(res: Int, pitch: Float = 1f) {
    val player = MediaPlayer.create(ctx, res, audio, session) ?: return
    if (pitch != 1f) setPitch(player, pitch)
    player.setOnCompletionListener { it.release() }
    player.start()
  }
}

private fun setPitch(player: MediaPlayer, pitch: Float) {
  player.playbackParams = player.playbackParams.setPitch(pitch)
  player.pause()
  player.seekTo(0)
}
