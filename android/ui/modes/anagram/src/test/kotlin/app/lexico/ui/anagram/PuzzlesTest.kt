package app.lexico.ui.anagram

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class PuzzlesTest {
  @Test fun easyNeedsSeveralAnagrams() = runBlocking {
    val single = FakeWords("CASO", "CASO")
    assertNull(Puzzles.fit(tiles("CASO"), single, Difficulty.EASY))
    assertNotNull(Puzzles.fit(tiles("CASO"), single, Difficulty.MEDIUM))
  }

  @Test fun tooManyAnagramsFixLetters() = runBlocking {
    val words = FakeWords("ARTES", "ARTES", "ASTER", "ESTAR", "RATES", "RESTA", "TASER", "TERSA")
    val p = Puzzles.fit(tiles("ARTES"), words, Difficulty.MEDIUM, Random(1))!!
    assertTrue(p.fixed.isNotEmpty())
    assertTrue(p.answers.size in 1..Difficulty.MAX_ANSWERS)
    assertTrue(p.answers.all { a -> p.fixed.all { a[it] == p.tiles[it] } })
  }

  @Test fun easyKeepsAllAnagramsOfShortWords() = runBlocking {
    val words = FakeWords("ARTES", "ARTES", "ASTER", "ESTAR", "RATES", "RESTA", "TASER", "TERSA")
    val p = Puzzles.fit(tiles("ARTES"), words, Difficulty.EASY, Random(1))!!
    assertEquals(emptySet<Int>(), p.fixed)
    assertEquals(7, p.answers.size)
  }

  @Test fun longWordsFitTheRack() = runBlocking {
    val p = Puzzles.fit(tiles("CASAMENTOS"), FakeWords("CASAMENTOS", "CASAMENTOS"), Difficulty.MEDIUM, Random(2))!!
    assertEquals(7, p.rack.size)
    assertEquals(3, p.fixed.size)
  }
}
