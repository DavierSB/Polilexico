package app.lexico.ui.lists

internal class WordTable private constructor(
  private val text: String,
  private val starts: IntArray,
  private val kinds: ByteArray,
  private val permilles: ShortArray,
  private val ownerStarts: IntArray,
  private val ownerList: IntArray,
) : AbstractList<Family>() {
  override val size: Int = kinds.size

  private val alphabetic: IntArray = (0 until size).sortedWith { a, b -> compare(a, word(b)) }.toIntArray()

  override fun get(index: Int): Family {
    val kind = kinds[index].toInt().toChar()
    return Family(kind != 'n', kind != 'v', 0, permilles[index].toInt(), "", listOf(word(index)), isWord = true, owners = ownersOf(index))
  }

  fun find(word: String): Family? {
    var low = 0
    var high = size - 1
    while (low <= high) {
      val mid = (low + high) ushr 1
      val c = compare(alphabetic[mid], word)
      if (c == 0) return get(alphabetic[mid])
      if (c < 0) low = mid + 1 else high = mid - 1
    }
    return null
  }

  fun wordsByFamily(families: Int): List<IntArray> {
    val sizes = IntArray(families)
    ownerList.forEach { if (it in 0 until families) sizes[it]++ }
    val out = List(families) { IntArray(sizes[it]) }
    val filled = IntArray(families)
    for (word in 0 until size) for (k in ownerStarts[word] until ownerStarts[word + 1]) {
      val owner = ownerList[k]
      if (owner in 0 until families) out[owner][filled[owner]++] = word
    }
    return out
  }

  private fun word(i: Int): String = text.substring(starts[i], starts[i + 1])

  private fun ownersOf(i: Int): IntArray = ownerList.copyOfRange(ownerStarts[i], ownerStarts[i + 1])

  private fun compare(i: Int, word: String): Int {
    val start = starts[i]
    val length = starts[i + 1] - start
    for (k in 0 until minOf(length, word.length)) {
      val d = text[start + k] - word[k]
      if (d != 0) return d
    }
    return length - word.length
  }

  companion object {
    fun parse(lines: List<String>): WordTable {
      val text = StringBuilder()
      val starts = IntArray(lines.size + 1)
      val ownerStarts = IntArray(lines.size + 1)
      val kinds = ByteArray(lines.size)
      val permilles = ShortArray(lines.size)
      val owners = ArrayList<Int>()
      var n = 0
      for (line in lines) {
        val c = line.split('\t').takeIf { it.size == 4 } ?: continue
        text.append(c[3])
        kinds[n] = c[0].first().code.toByte()
        permilles[n] = c[1].toInt().toShort()
        c[2].split(',').filter { it.isNotEmpty() }.forEach { owners += it.toInt() }
        starts[n + 1] = text.length
        ownerStarts[n + 1] = owners.size
        n++
      }
      return WordTable(text.toString(), starts.copyOf(n + 1), kinds.copyOf(n), permilles.copyOf(n), ownerStarts.copyOf(n + 1), owners.toIntArray())
    }
  }
}
