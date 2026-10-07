package app.lexico.game

import app.lexico.go.events.Events

enum class Cue {
  WARNING, LAST_SECONDS, TIME_UP, CORRECT, WRONG, MISS, OPPONENT, CELEBRATION, GAME_OVER;

  internal companion object {
    fun of(text: String): Cue? = when (text) {
      Events.CueWarning -> WARNING
      Events.CueLastSeconds -> LAST_SECONDS
      Events.CueTimeUp -> TIME_UP
      Events.CueCorrect -> CORRECT
      Events.CueWrong -> WRONG
      Events.CueMiss -> MISS
      Events.CueOpponent -> OPPONENT
      Events.CueVictory -> CELEBRATION
      Events.CueGameOver -> GAME_OVER
      else -> null
    }
  }
}

internal data class CueReading(val count: Long, val text: String)
