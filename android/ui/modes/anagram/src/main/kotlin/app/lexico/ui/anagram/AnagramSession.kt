package app.lexico.ui.anagram

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.lexico.ui.recall.Record
import app.lexico.ui.recall.RecordBook
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random
import kotlin.time.TimeSource

private const val TICK_MS = 100L
private const val WARNING_MS = 30_000L
private const val LAST_SECONDS_MS = 5_000L

sealed interface Stage {
  data object Loading : Stage

  data class Solving(val puzzle: Puzzle, val found: List<List<String>> = emptyList()) : Stage

  data class Revealed(val puzzle: Puzzle, val found: List<List<String>>, val outcome: Outcome) : Stage

  data class Finished(val puzzle: Puzzle?, val found: List<List<String>>, val outcome: Outcome) : Stage

  data class Failed(val message: String) : Stage
}

enum class Outcome { SOLVED, GAVE_UP, NO_LIVES, TIMEOUT }

enum class Reply { SOLVED, FOUND, REPEATED, INVALID }

enum class Signal { HIT, WRONG, WARNING, LAST_SECONDS, TIME_UP }

@Stable
class AnagramSession(
  val config: AnagramConfig,
  private val source: AnagramSource,
  private val records: RecordBook,
  private val scope: CoroutineScope,
  private val random: Random = Random.Default,
) {
  var stage: Stage by mutableStateOf(Stage.Loading)
    private set
  var lives by mutableIntStateOf(config.lives)
    private set
  var solved by mutableIntStateOf(0)
    private set
  var hand by mutableIntStateOf(0)
    private set
  var remainingMs by mutableLongStateOf(config.totalMs)
    private set
  var paused by mutableStateOf(false)
    private set
  var best by mutableIntStateOf(records.best())
    private set
  var record: Record? by mutableStateOf(null)
    private set
  var signal: Signal? by mutableStateOf(null)
    private set
  var signals by mutableIntStateOf(0)
    private set

  private var next: Deferred<Result<Puzzle>> = fetch()

  init {
    startHand()
    scope.launch { runClock() }
  }

  fun submit(letters: List<String>): Reply? {
    val s = stage as? Stage.Solving ?: return null
    return when (letters) {
      !in s.puzzle.answers -> miss(s)
      in s.found -> Reply.REPEATED
      else -> hit(s, s.found + listOf(letters))
    }
  }

  fun giveUp() {
    val s = stage as? Stage.Solving ?: return
    loseLife(s, Outcome.GAVE_UP)
  }

  fun next() {
    if (stage is Stage.Revealed) startHand()
  }

  fun retry() {
    if (stage is Stage.Failed) startHand()
  }

  fun pause() {
    if (stage is Stage.Solving) paused = true
  }

  fun resume() {
    paused = false
  }

  fun restart() {
    lives = config.lives
    solved = 0
    hand = 0
    remainingMs = config.totalMs
    best = records.best()
    record = null
    paused = false
    startHand()
  }

  private fun hit(s: Stage.Solving, found: List<List<String>>): Reply {
    emit(Signal.HIT)
    if (config.difficulty.findAll && found.size < s.puzzle.answers.size) {
      stage = s.copy(found = found)
      return Reply.FOUND
    }
    solved++
    stage = Stage.Revealed(s.puzzle, found, Outcome.SOLVED)
    return Reply.SOLVED
  }

  private fun miss(s: Stage.Solving): Reply {
    lives--
    emit(Signal.WRONG)
    if (lives == 0) finish(s, Outcome.NO_LIVES)
    return Reply.INVALID
  }

  private fun loseLife(s: Stage.Solving, outcome: Outcome) {
    lives--
    emit(Signal.WRONG)
    if (lives == 0) finish(s, Outcome.NO_LIVES) else stage = Stage.Revealed(s.puzzle, s.found, outcome)
  }

  private fun finish(s: Stage.Solving?, outcome: Outcome) {
    record = records.submit(solved)
    stage = Stage.Finished(s?.puzzle, s?.found.orEmpty(), outcome)
  }

  private fun emit(s: Signal) {
    signal = s
    signals++
  }

  private suspend fun runClock() {
    var last = TimeSource.Monotonic.markNow()
    while (true) {
      delay(TICK_MS)
      val elapsed = last.elapsedNow().inWholeMilliseconds
      last = TimeSource.Monotonic.markNow()
      if (stage is Stage.Solving && !paused) tick(elapsed)
    }
  }

  private fun tick(elapsed: Long) {
    val before = remainingMs
    remainingMs = maxOf(0, before - elapsed)
    when {
      remainingMs == 0L -> timeUp()
      crossed(before, WARNING_MS) -> emit(Signal.WARNING)
      crossed(before, LAST_SECONDS_MS) -> emit(Signal.LAST_SECONDS)
    }
  }

  private fun crossed(before: Long, mark: Long): Boolean = before > mark && remainingMs <= mark

  private fun timeUp() {
    emit(Signal.TIME_UP)
    finish(stage as? Stage.Solving, Outcome.TIMEOUT)
  }

  private fun startHand() {
    stage = Stage.Loading
    val pending = next
    next = fetch()
    scope.launch { pending.await().fold(::pose) { stage = Stage.Failed(it.message ?: it.toString()) } }
  }

  private fun pose(puzzle: Puzzle) {
    hand++
    stage = Stage.Solving(puzzle)
  }

  private fun fetch(): Deferred<Result<Puzzle>> =
    scope.async(Dispatchers.Default) { runCatching { Puzzles.next(source, config.difficulty, random) } }
}
