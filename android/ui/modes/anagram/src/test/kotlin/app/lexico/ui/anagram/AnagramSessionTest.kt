package app.lexico.ui.anagram

import app.lexico.ui.recall.Record
import app.lexico.ui.recall.RecordBook
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AnagramSessionTest {
  private class Book(var stored: Int = 0) : RecordBook {
    override fun best(): Int = stored

    override fun submit(recalled: Int): Record = Record(maxOf(stored, recalled), recalled > stored).also { stored = it.best }
  }

  @Test fun mediumIsSolvedWithOneAnagram() = session(Difficulty.MEDIUM) { s ->
    assertEquals(Reply.SOLVED, s.submit(tiles("SACO")))
    assertEquals(1, s.solved)
    assertTrue(s.stage is Stage.Revealed)
  }

  @Test fun hardNeedsEveryAnagram() = session(Difficulty.HARD) { s ->
    assertEquals(Reply.FOUND, s.submit(tiles("CAOS")))
    assertEquals(Reply.REPEATED, s.submit(tiles("CAOS")))
    assertEquals(3, s.lives)
    assertEquals(Reply.SOLVED, s.submit(tiles("SACO")))
    assertEquals(1, s.solved)
  }

  @Test fun invalidWordsCostLivesUntilTheEnd() = session(Difficulty.MEDIUM) { s ->
    repeat(2) { assertEquals(Reply.INVALID, s.submit(tiles("OSCA"))) }
    assertTrue(s.stage is Stage.Solving)
    s.submit(tiles("OSCA"))
    assertEquals(0, s.lives)
    assertEquals(Outcome.NO_LIVES, (s.stage as Stage.Finished).outcome)
  }

  @Test fun givingUpRevealsTheAnswers() = session(Difficulty.MEDIUM) { s ->
    s.giveUp()
    assertEquals(2, s.lives)
    assertEquals(Outcome.GAVE_UP, (s.stage as Stage.Revealed).outcome)
  }

  @Test fun raceNeverRunsOutOfLives() = session(Difficulty.MEDIUM, lives = 0) { s ->
    repeat(5) { assertEquals(Reply.INVALID, s.submit(tiles("OSCA"))) }
    assertTrue(s.stage is Stage.Solving)
    s.giveUp()
    assertEquals(Outcome.GAVE_UP, (s.stage as Stage.Revealed).outcome)
  }

  private fun session(difficulty: Difficulty, lives: Int = AnagramConfig.DEFAULT_LIVES, test: suspend (AnagramSession) -> Unit) = runBlocking {
    val scope = CoroutineScope(coroutineContext + Job())
    val s = AnagramSession(AnagramConfig(lives = lives, difficulty = difficulty), FakeWords("SACO", "SACO", "CAOS"), Book(), scope)
    while (s.stage !is Stage.Solving) yield()
    test(s)
    scope.cancel()
  }
}
