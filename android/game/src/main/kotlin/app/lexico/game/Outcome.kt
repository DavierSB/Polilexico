package app.lexico.game

/** Como termino una partida, para ti. */
enum class Outcome {
  WIN, LOSS, TIE;

  internal companion object {
    /** "win", "loss" o "tie", como lo escribe el motor. */
    fun of(text: String): Outcome = when (text) {
      "win" -> WIN
      "loss" -> LOSS
      else -> TIE
    }
  }
}
