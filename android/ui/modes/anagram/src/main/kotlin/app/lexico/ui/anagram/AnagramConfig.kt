package app.lexico.ui.anagram

const val DEFAULT_TOTAL_MS = 300_000L

enum class Difficulty(val key: String, val label: String, val findAll: Boolean) {
  EASY("easy", "Fácil", false), MEDIUM("medium", "Medio", false), HARD("hard", "Difícil", true);

  fun accepts(answers: Int): Boolean = if (this == EASY) answers >= 2 else answers in 1..MAX_ANSWERS

  companion object {
    const val MAX_ANSWERS = 5
  }
}

data class AnagramConfig(
  val totalMs: Long = DEFAULT_TOTAL_MS,
  val lives: Int = DEFAULT_LIVES,
  val difficulty: Difficulty = Difficulty.MEDIUM,
) {
  val limited: Boolean get() = lives > 0

  companion object {
    const val DEFAULT_LIVES = 3
    val LIVES = 1..5
  }
}
