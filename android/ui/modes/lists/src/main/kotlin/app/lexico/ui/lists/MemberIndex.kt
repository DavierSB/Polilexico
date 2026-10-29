package app.lexico.ui.lists

internal class MemberIndex(private val families: List<Family>) {
  private val words: Array<String>
  private val owners: IntArray

  init {
    val pairs = families.flatMapIndexed { i, f -> f.words.map { it to i } }.sortedWith(compareBy({ it.first }, { it.second }))
    words = Array(pairs.size) { pairs[it].first }
    owners = IntArray(pairs.size) { pairs[it].second }
  }

  fun find(word: String): Family? {
    val at = lowerBound(word)
    return if (at < words.size && words[at] == word) families[owners[at]] else null
  }

  private fun lowerBound(word: String): Int {
    var low = 0
    var high = words.size
    while (low < high) {
      val mid = (low + high) ushr 1
      if (words[mid] < word) low = mid + 1 else high = mid
    }
    return low
  }
}
