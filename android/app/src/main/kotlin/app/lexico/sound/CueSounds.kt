package app.lexico.sound

import android.content.Context
import app.lexico.game.Cue
import app.lexico.sounds.Sound
import app.lexico.sounds.SoundPlayer

internal class CueSounds(ctx: Context) {
  private val player = SoundPlayer(ctx)

  fun play(cue: Cue) = player.play(soundOf(cue))

  fun click() = player.play(Sound.CLICK)
}

private fun soundOf(cue: Cue): Sound = when (cue) {
  Cue.WARNING -> Sound.WARNING
  Cue.LAST_SECONDS -> Sound.LAST_SECONDS
  Cue.TIME_UP -> Sound.TIME_UP
  Cue.CORRECT -> Sound.CORRECT
  Cue.MISS -> Sound.MISS
  Cue.WRONG -> Sound.WRONG
  Cue.OPPONENT -> Sound.OPPONENT
  Cue.CELEBRATION -> Sound.CELEBRATION
  Cue.GAME_OVER -> Sound.GAME_OVER
}
