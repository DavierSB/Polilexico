package app.lexico.game

enum class Outcome {
  WIN, LOSS, TIE;

  internal companion object {
    fun of(text: String): Outcome = when (text) {
      "win" -> WIN
      "loss" -> LOSS
      else -> TIE
    }
  }
}
