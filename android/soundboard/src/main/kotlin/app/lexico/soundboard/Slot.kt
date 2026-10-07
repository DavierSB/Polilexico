package app.lexico.soundboard

import app.lexico.sounds.Sound

enum class Slot(val title: String, val use: String) {
  WARNING(Sound.WARNING.title, Sound.WARNING.use),
  LAST_SECONDS(Sound.LAST_SECONDS.title, Sound.LAST_SECONDS.use),
  TIME_UP(Sound.TIME_UP.title, Sound.TIME_UP.use),
  CORRECT(Sound.CORRECT.title, Sound.CORRECT.use),
  WRONG(Sound.WRONG.title, Sound.WRONG.use),
  MISS(Sound.MISS.title, Sound.MISS.use),
  OPPONENT(Sound.OPPONENT.title, Sound.OPPONENT.use),
  CELEBRATION(Sound.CELEBRATION.title, Sound.CELEBRATION.use),
  GAME_OVER(Sound.GAME_OVER.title, Sound.GAME_OVER.use),
  CLICK(Sound.CLICK.title, Sound.CLICK.use),
}
