package app.lexico.ui.recall

import kotlin.random.Random

object FixedTiles {
  const val RACK_SIZE = 7

  fun choose(tiles: List<String>, random: Random = Random.Default, isWord: (String) -> Boolean = { false }): Set<Int> {
    val count = tiles.size - RACK_SIZE
    if (count <= 0) return emptySet()
    val valid = memoized(isWord)
    return subsets(tiles.size, count).filter { fixed -> runsAreWords(tiles, fixed, valid) }.toList().random(random)
  }

  fun runs(fixed: Set<Int>): List<List<Int>> =
    fixed.sorted().fold(mutableListOf<MutableList<Int>>()) { runs, i ->
      runs.apply { if (lastOrNull()?.last() == i - 1) last() += i else add(mutableListOf(i)) }
    }

  private fun runsAreWords(tiles: List<String>, fixed: Set<Int>, isWord: (String) -> Boolean): Boolean =
    runs(fixed).all { run -> run.size == 1 || isWord(dictionaryText(run.map(tiles::get))) }

  private fun subsets(n: Int, size: Int): Sequence<Set<Int>> =
    (0 until (1 shl n)).asSequence()
      .filter { Integer.bitCount(it) == size }
      .map { mask -> (0 until n).filter { mask and (1 shl it) != 0 }.toSet() }

  private fun memoized(isWord: (String) -> Boolean): (String) -> Boolean {
    val known = HashMap<String, Boolean>()
    return { word -> known.getOrPut(word) { isWord(word) } }
  }
}
