package app.lexico.ui.recall

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class WordsTest {
  private val game = listOf(
    ScoredPlacement("H8 CASA", 12),
    ScoredPlacement("8H .ERO", 10), // pasa por la C: CERO
    ScoredPlacement("K5 LOB.", 20), // termina en la O de CERO: LOBO
    ScoredPlacement("5K .UNA", 15), // LUNA
    ScoredPlacement("A1 SI", 30), // muy corta
    ScoredPlacement("C1 [CH]Aa", 12), // digrafo y comodin
    ScoredPlacement("E1 CASA", 12), // repetida
    ScoredPlacement("G1 CERO", 9), // repetida, con menos puntos
    ScoredPlacement("(pasa)", 0), // no es una colocacion
  )

  @Test fun mainWordsFillLettersFromTheBoard() {
    val words = Words.mainWords(game).map { (tiles, score) -> tiles.joinToString("") to score }
    assertEquals("CERO" to 10, words[1])
    assertEquals("LOBO" to 20, words[2])
    assertEquals("LUNA" to 15, words[3])
    assertEquals(8, words.size)
  }

  @Test fun digraphsAreOneTileAndBlanksShowTheirLetter() {
    val (tiles, _) = Words.mainWords(game)[5]
    assertEquals(listOf("CH", "A", "A"), tiles)
  }

  @Test fun pickTakesTheBestDistinctLongWords() {
    val picked = Words.pick(game, 3, Random(1))
    assertEquals(setOf("LOBO", "LUNA", "CASA"), picked.map { it.text }.toSet())
    assertEquals(12, picked.single { it.text == "CASA" }.score)
  }

  @Test fun pickKeepsTheBestScoreOfARepeatedWord() {
    val picked = Words.pick(game, 10, Random(1))
    assertEquals(listOf("CASA", "CERO", "CHAA", "LOBO", "LUNA"), picked.map { it.text }.sorted())
    assertEquals(10, picked.single { it.text == "CERO" }.score)
  }

  @Test fun scrambledHasTheSameTilesInAnotherOrder() {
    for (word in Words.pick(game, 10, Random(7))) {
      assertTrue(word.fixed.isEmpty())
      assertEquals(word.tiles.sorted(), word.scrambled.sorted())
      assertNotEquals(word.tiles, word.scrambled)
    }
  }

  @Test fun longWordsLeaveARackOfSevenTiles() {
    val long = listOf(ScoredPlacement("H2 CAMPANEROS", 97), ScoredPlacement("A1 ELEFANTES", 50))
    val words = Words.pick(long, 2, Random(3))
    assertEquals(2, words.size)
    for (word in words) {
      assertEquals(FixedTiles.RACK_SIZE, word.scrambled.size)
      assertEquals(word.tiles.size - FixedTiles.RACK_SIZE, word.fixed.size)
    }
  }
}
