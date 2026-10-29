package app.lexico.ui.anagram

import app.lexico.ui.recall.AnswerBoard
import app.lexico.ui.recall.FixedTiles
import app.lexico.ui.recall.Words
import kotlin.random.Random

interface AnagramSource {
  suspend fun randomWord(minTiles: Int, maxTiles: Int): List<String>

  suspend fun anagrams(tiles: List<String>): List<List<String>>

  fun isWord(word: String): Boolean = false
}

data class Puzzle(val tiles: List<String>, val fixed: Set<Int>, val rack: List<String>, val answers: List<List<String>>) {
  fun board(): AnswerBoard = AnswerBoard(fixed.associateWith(tiles::get), rack, List(tiles.size) { null })
}

object Puzzles {
  const val MIN_TILES = 4
  const val MAX_TILES = 9
  private const val MIN_RACK = 3
  private const val MAX_TRIES = 300

  suspend fun next(source: AnagramSource, difficulty: Difficulty, random: Random = Random.Default): Puzzle {
    repeat(MAX_TRIES) { fit(source.randomWord(MIN_TILES, MAX_TILES), source, difficulty, random)?.let { return it } }
    error("no se encontró una palabra para esta dificultad")
  }

  suspend fun fit(word: List<String>, source: AnagramSource, difficulty: Difficulty, random: Random = Random.Default): Puzzle? {
    val all = source.anagrams(word).distinct()
    if (word !in all) return null
    return fixedCounts(word.size, difficulty).firstNotNullOfOrNull { count -> withFixed(word, count, all, source, difficulty, random) }
  }

  private fun withFixed(
    word: List<String>, count: Int, all: List<List<String>>, source: AnagramSource, difficulty: Difficulty, random: Random,
  ): Puzzle? =
    FixedTiles.options(word, count, source::isWord).toList().shuffled(random)
      .map { fixed -> puzzle(word, fixed, all, random) }
      .firstOrNull { difficulty.accepts(it.answers.size) }

  private fun fixedCounts(size: Int, difficulty: Difficulty): IntRange {
    val first = maxOf(0, size - FixedTiles.RACK_SIZE)
    return if (difficulty == Difficulty.EASY) first..first else first..maxOf(first, size - MIN_RACK)
  }

  private fun puzzle(word: List<String>, fixed: Set<Int>, all: List<List<String>>, random: Random): Puzzle {
    val answers = all.filter { a -> fixed.all { a[it] == word[it] } }
    val rack = word.filterIndexed { i, _ -> i !in fixed }
    return Puzzle(word, fixed, Words.scramble(rack, random), answers)
  }
}
