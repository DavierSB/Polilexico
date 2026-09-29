package app.lexico.ui.recall

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class FixedTilesTest {
  private fun tiles(word: String) = word.map { it.toString() }

  @Test fun wordsThatFitTheRackHaveNoFixedTiles() {
    assertEquals(emptySet<Int>(), FixedTiles.choose(tiles("CASAMOS"), Random(1)))
  }

  @Test fun withoutDictionaryFixedTilesAreApart() {
    repeat(50) { seed ->
      val fixed = FixedTiles.choose(tiles("ELEFANTES"), Random(seed))
      assertEquals(2, fixed.size)
      assertTrue(FixedTiles.runs(fixed).all { it.size == 1 })
    }
  }

  @Test fun fifteenTilesFixEveryOtherOne() {
    val fixed = FixedTiles.choose(tiles("ABCDEFGHIJKLMNO"), Random(1))
    assertEquals((0..14 step 2).toSet(), fixed)
  }

  @Test fun adjacentFixedTilesMustFormAWord() {
    val asked = mutableSetOf<String>()
    val isWord = { w: String -> asked += w; w == "FA" }
    repeat(50) { seed ->
      val word = tiles("ELEFANTES")
      val runs = FixedTiles.runs(FixedTiles.choose(word, Random(seed), isWord))
      assertTrue(runs.all { it.size == 1 || it.joinToString("") { i -> word[i] } == "FA" })
    }
    assertTrue("FA" in asked)
  }

  @Test fun digraphsGoInBracketsForTheDictionary() {
    val asked = mutableSetOf<String>()
    FixedTiles.choose(listOf("CH", "A", "R", "R", "O", "S", "A", "S", "E"), Random(1)) { asked += it; false }
    assertTrue("[CH]A" in asked)
  }

  @Test fun runsGroupConsecutiveSquares() {
    assertEquals(listOf(listOf(0, 1), listOf(3), listOf(5, 6, 7)), FixedTiles.runs(setOf(7, 0, 5, 1, 3, 6)))
  }
}
