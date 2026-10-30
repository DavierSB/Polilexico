package app.lexico.game

enum class Outcome {
  WIN, LOSS, TIE;

  fun reversed(): Outcome = when (this) {
    WIN -> LOSS
    LOSS -> WIN
    TIE -> TIE
  }

  internal companion object {
    fun of(text: String): Outcome = when (text) {
      "win" -> WIN
      "loss" -> LOSS
      else -> TIE
    }
  }
}
