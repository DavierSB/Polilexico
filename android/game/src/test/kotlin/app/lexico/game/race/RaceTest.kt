package app.lexico.game.race

import app.lexico.game.modes.recall.ScoredPlacement
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RaceTest {
  private var clock = 0L

  private fun race(goal: Int = 0) =
    Race(RaceSetup(RaceGame.ANAGRAM, goal = goal), listOf("Ana", "Luis", "Eva"), CoroutineScope(Dispatchers.Unconfined)) { clock }

  @Test fun reachingTheGoalEndsTheRace() {
    val r = race(goal = 3)
    r.report("Luis", 2, 0, true)
    assertFalse(r.view.value.over)
    r.report("Ana", 3, 0, false)
    assertTrue(r.view.value.over)
    assertEquals(listOf("Ana", "Luis", "Eva"), r.view.value.runners.map { it.name })
  }

  @Test fun whoRanOutOfLivesStillRanksByScore() {
    val r = race()
    r.report("Luis", 2, 0, true)
    r.report("Ana", 1, 0, false)
    assertFalse(r.view.value.over)
    assertEquals(listOf("Luis", "Ana", "Eva"), r.view.value.runners.map { it.name })
  }

  @Test fun tiesGoToWhoGotThereFirst() {
    val r = race()
    clock = 10; r.report("Eva", 2, 0, false)
    clock = 20; r.report("Luis", 2, 0, false)
    assertEquals(listOf("Eva", "Luis", "Ana"), r.view.value.runners.map { it.name })
  }

  @Test fun pointsBreakTiesBeforeTime() {
    val r = race()
    clock = 10; r.report("Eva", 2, 30, true)
    clock = 20; r.report("Luis", 2, 50, true)
    assertEquals("Luis", r.view.value.runners.first().name)
  }

  @Test fun endsWhenEveryonePresentIsDone() {
    val r = race()
    r.report("Ana", 1, 0, true)
    r.report("Luis", 4, 0, true)
    assertFalse(r.view.value.over)
    r.setPresent("Eva", false)
    assertTrue(r.view.value.over)
  }

  @Test fun reportsAfterTheEndAreIgnored() {
    val r = race()
    r.finish()
    r.report("Luis", 5, 0, false)
    assertEquals(0, r.view.value.runners.first { it.name == "Luis" }.score)
  }

  private var games = 0

  private fun rounds(count: Int) = Race(
    RaceSetup(RaceGame.RECALL, rounds = count), listOf("Ana", "Luis", "Eva"), CoroutineScope(Dispatchers.Unconfined),
    nextGame = { games++; listOf(ScoredPlacement("h8 CASA", games)) },
  ) { clock }

  @Test fun everyoneGetsTheSameRoundGame() {
    val r = rounds(3)
    assertEquals(RaceRound(1, open = true, listOf(ScoredPlacement("h8 CASA", 1))), r.view.value.round)
  }

  @Test fun theFirstToFinishClosesTheRoundForAll() {
    val r = rounds(3)
    r.finishRound("Luis", 1)
    assertFalse(r.view.value.round!!.open)
    r.finishRound("Ana", 1)
    assertEquals(1, r.view.value.round!!.number)
  }

  @Test fun theNextRoundWaitsForEveryone() {
    val r = rounds(3)
    r.finishRound("Luis", 1)
    r.report("Eva", 0, 0, done = true)
    r.ready("Ana")
    r.ready("Luis")
    assertEquals(1, r.view.value.round!!.number)
    r.ready("Eva")
    assertEquals(RaceRound(2, open = true, listOf(ScoredPlacement("h8 CASA", 2))), r.view.value.round)
    assertFalse(r.view.value.runners.any { it.ready || it.done })
  }

  @Test fun whoIsReadyStaysDoneEvenWithFreshLives() {
    val r = rounds(3)
    r.report("Ana", 0, 0, done = true)
    r.ready("Ana")
    r.report("Ana", 0, 0, done = false, lives = 3)
    r.report("Luis", 0, 0, done = true)
    r.report("Eva", 0, 0, done = true)
    assertFalse(r.view.value.round!!.open)
  }

  @Test fun whenNobodyHasLivesLeftOnlyTheRoundEnds() {
    val r = rounds(3)
    listOf("Ana", "Luis", "Eva").forEach { r.report(it, 1, 0, done = true) }
    assertFalse(r.view.value.over)
    assertFalse(r.view.value.round!!.open)
  }

  @Test fun closingTheLastRoundEndsTheRace() {
    val r = rounds(1)
    r.finishRound("Eva", 1)
    assertTrue(r.view.value.over)
  }
}
