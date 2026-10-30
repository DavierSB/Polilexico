package app.lexico.game.race

import app.lexico.game.modes.recall.ScoredPlacement

enum class RaceGame(val key: String) { ANAGRAM("anagram"), SPRINT("sprint"), RECALL("recall") }

data class RaceSetup(
  val game: RaceGame,
  val timeMs: Long = 0,
  val goal: Int = 0,
  val rounds: Int = 0,
  val difficulty: String = "",
  val intervalMs: Long = 0,
  val single: Boolean = true,
  val roundMs: Long = 0,
)

data class Runner(
  val name: String, val score: Int, val points: Int, val done: Boolean, val present: Boolean, val lives: Int = 0, val ready: Boolean = false,
)

data class RaceRound(val number: Int, val open: Boolean, val game: List<ScoredPlacement>, val remainingMs: Long? = null)

data class RaceView(val setup: RaceSetup, val over: Boolean, val remainingMs: Long?, val runners: List<Runner>, val round: RaceRound? = null)
