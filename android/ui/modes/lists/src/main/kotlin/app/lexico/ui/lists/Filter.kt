package app.lexico.ui.lists

const val SIMPLE_LETTERS = "aeioucdlnrst"

private const val VOWELS = "aeiou"

data class Filter(
  val minLength: Int = MIN_TILES,
  val maxLength: Int = MAX_TILES,
  val contains: String = "",
  val fixed: Map<Int, Char> = emptyMap(),
  val fixedEnd: Map<Int, Char> = emptyMap(),
  val kind: Kind = Kind.ALL,
  val vowels: Int? = null,
  val consonants: Int? = null,
  val simpleOnly: Boolean = false,
) {
  private val need: Map<Char, Int> = contains.groupingBy { it }.eachCount()

  fun matches(family: Family): Boolean =
    kindMatches(family) && shortest(family) in minLength..maxLength && family.words.any(::shapeMatches)

  fun matches(word: String): Boolean = word.length in minLength..maxLength && shapeMatches(word)

  private fun shortest(family: Family): Int = family.words.minOf { it.length } - if (family.verbal && !family.isWord) 1 else 0

  private fun shapeMatches(word: String): Boolean = positionsMatch(word) && countsMatch(word) && lettersMatch(word)

  private fun kindMatches(family: Family): Boolean = when (kind) {
    Kind.ALL -> true
    Kind.VERBAL -> family.verbal
    Kind.NON_VERBAL -> family.nonVerbal
  }

  private fun positionsMatch(word: String): Boolean =
    fixed.all { (i, c) -> word.getOrNull(i) == c } && fixedEnd.all { (i, c) -> word.getOrNull(word.length - 1 - i) == c }

  private fun countsMatch(word: String): Boolean {
    val v = vowelsIn(word)
    return (vowels == null || v == vowels) && (consonants == null || word.length - v == consonants)
  }

  private fun lettersMatch(word: String): Boolean {
    val counts = word.groupingBy { it }.eachCount()
    if (need.any { (c, n) -> (counts[c] ?: 0) < n }) return false
    return !simpleOnly || counts.all { (c, n) -> c in SIMPLE_LETTERS || n <= (need[c] ?: 0) }
  }
}

fun vowelsIn(word: String): Int = word.count { it in VOWELS }
