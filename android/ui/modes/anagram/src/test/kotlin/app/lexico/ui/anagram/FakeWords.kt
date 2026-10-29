package app.lexico.ui.anagram

internal class FakeWords(private val drawn: String, vararg words: String) : AnagramSource {
  private val lexicon = words.map(::tiles)

  override suspend fun randomWord(minTiles: Int, maxTiles: Int): List<String> = tiles(drawn)

  override suspend fun anagrams(tiles: List<String>): List<List<String>> = lexicon.filter { it.sorted() == tiles.sorted() }

  override fun isWord(word: String): Boolean = true
}

internal fun tiles(word: String): List<String> = word.map(Char::toString)
