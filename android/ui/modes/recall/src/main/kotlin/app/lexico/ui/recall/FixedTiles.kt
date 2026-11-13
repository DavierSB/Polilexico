package app.lexico.ui.recall

import kotlin.random.Random

object FixedTiles {
  const val RACK_SIZE = 7

  fun choose(
    tiles: List<String>, random: Random = Random.Default, required: Set<Int> = emptySet(), isWord: (String) -> Boolean = { false },
  ): Set<Int> {
    val count = maxOf(tiles.size - RACK_SIZE, required.size)
    if (count <= 0) return emptySet()
    return options(tiles, count, isWord, required).toList().random(random)
  }

  fun options(tiles: List<String>, count: Int, isWord: (String) -> Boolean, required: Set<Int> = emptySet()): Sequence<Set<Int>> {
    val valid = memoized(isWord)
    return subsets(tiles.size, count).filter { fixed -> fixed.containsAll(required) && runsAreWords(tiles, fixed, valid, required) }
  }

  fun runs(fixed: Set<Int>): List<List<Int>> =
    fixed.sorted().fold(mutableListOf<MutableList<Int>>()) { runs, i ->
      runs.apply { if (lastOrNull()?.last() == i - 1) last() += i else add(mutableListOf(i)) }
    }

  private fun runsAreWords(tiles: List<String>, fixed: Set<Int>, isWord: (String) -> Boolean, required: Set<Int>): Boolean =
    runs(fixed).all { run -> run.size == 1 || run.any { it in required } || isWord(dictionaryText(run.map(tiles::get))) }

  private fun subsets(n: Int, size: Int): Sequence<Set<Int>> =
    (0 until (1 shl n)).asSequence()
      .filter { Integer.bitCount(it) == size }
      .map { mask -> (0 until n).filter { mask and (1 shl it) != 0 }.toSet() }

  private fun memoized(isWord: (String) -> Boolean): (String) -> Boolean {
    val known = HashMap<String, Boolean>()
    return { word -> known.getOrPut(word) { isWord(word) } }
  }
}
