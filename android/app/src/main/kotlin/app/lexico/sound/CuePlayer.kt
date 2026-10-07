package app.lexico.sound

import android.content.Context
import app.lexico.game.Cue
import app.lexico.menu.Settings

class CuePlayer(ctx: Context, private val settings: Settings) {
  private val profile = PhoneProfile(ctx)
  private val sounds = CueSounds(ctx)
  private val vibrations = CueVibrations(ctx)

  fun play(cue: Cue) {
    if (settings.sounds && profile.allowsSound()) sounds.play(cue)
    if (settings.vibration && profile.allowsVibration()) vibrations.play(cue)
  }

  fun click() {
    if (settings.sounds && profile.allowsSound()) sounds.click()
  }
}
