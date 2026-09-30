package app.lexico.ui.recall

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Una serie con una partida de dos palabras (CASA y CERO), y un diccionario con ellas y sus anagramas SACA y CORE. */
class RecallSessionTest {
  private val game = listOf(ScoredPlacement("h8 CASA", 12), ScoredPlacement("8h .ERO", 10))

  private val source = object : GameSource {
    override suspend fun game(): List<ScoredPlacement> = this@RecallSessionTest.game

    override fun isWord(word: String): Boolean = word in setOf("CASA", "SACA", "CERO", "CORE")
  }

  private class Book(var stored: Int = 0) : RecordBook {
    override fun best(): Int = stored

    override fun submit(recalled: Int): Record = Record(maxOf(stored, recalled), recalled > stored).also { stored = it.best }
  }

  @Test fun hitCountsAndKeepsLives() = session { s, _ ->
    assertEquals(Submission.Resolved(Verdict.HIT), s.submit(s.words[0].tiles))
    assertEquals(1, s.recalled)
    assertEquals(3, s.lives)
  }

  @Test fun validButNotPlayedCostsALife() = session { s, _ ->
    val anagram = if (s.words[0].text == "CASA") listOf("S", "A", "C", "A") else listOf("C", "O", "R", "E")
    assertEquals(Submission.Resolved(Verdict.NOT_PLAYED), s.submit(anagram))
    assertEquals(2, s.lives)
  }

  @Test fun invalidInSingleCostsALife() = session { s, _ ->
    assertEquals(Submission.Resolved(Verdict.INVALID), s.submit(listOf("X", "X", "X", "X")))
    assertEquals(2, s.lives)
  }

  @Test fun invalidInVoidIsRejectedForFree() = session(RecallConfig(single = false)) { s, _ ->
    assertEquals(Submission.Rejected, s.submit(listOf("X", "X", "X", "X")))
    assertEquals(3, s.lives)
    assertNull((s.stage as Stage.Solving).verdict)
  }

  @Test fun gameEndsWithLivesLeft() = session { s, _ ->
    s.forget()
    s.next()
    s.submit(s.words[1].tiles)
    s.next()
    assertEquals(Stage.RoundDone, s.stage)
    assertEquals(2, s.lives)
  }

  @Test fun noLivesEndsTheSeriesAndKeepsTheRecord() = session(RecallConfig(lives = 1)) { s, book ->
    s.submit(s.words[0].tiles)
    s.next()
    s.forget()
    s.next()
    assertEquals(Stage.SeriesDone, s.stage)
    assertEquals(Record(1, isNew = true), s.record)
    assertEquals(1, book.stored)
  }

  /** Una serie ya en la primera palabra, sobre un diccionario de prueba. */
  private fun session(config: RecallConfig = RecallConfig(), test: suspend (RecallSession, Book) -> Unit) = runBlocking {
    val scope = CoroutineScope(coroutineContext + Job())
    val book = Book()
    val s = RecallSession(config, source, book, scope)
    while (s.stage !is Stage.Watching) yield()
    s.watched()
    test(s, book)
    scope.cancel()
  }
}
