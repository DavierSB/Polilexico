package app.lexico.ui.recall

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random

interface GameSource {
  suspend fun game(): List<ScoredPlacement>

  fun isWord(word: String): Boolean = false
}

interface RecordBook {
  fun best(): Int

  fun submit(recalled: Int): Record
}

data class Record(val best: Int, val isNew: Boolean)

private class RoundGame(val game: List<ScoredPlacement>, val words: List<WordToRecall>)

sealed interface Stage {
  data object Loading : Stage

  data class Watching(val game: List<ScoredPlacement>) : Stage

  data class Solving(val index: Int, val verdict: Verdict? = null) : Stage

  data object RoundDone : Stage

  data object SeriesDone : Stage

  data class Failed(val message: String) : Stage
}

enum class Verdict {
  HIT,

  INVALID,

  NOT_PLAYED,

  FORGOTTEN,
}

sealed interface Submission {
  data class Resolved(val verdict: Verdict) : Submission

  data object Rejected : Submission
}

data class Answer(val word: WordToRecall, val hit: Boolean)

@Stable
class RecallSession(
  val config: RecallConfig,
  private val source: GameSource,
  private val records: RecordBook,
  private val scope: CoroutineScope,
  private val random: Random = Random.Default,
) {
  var stage: Stage by mutableStateOf(Stage.Loading)
    private set

  var words: List<WordToRecall> by mutableStateOf(emptyList())
    private set

  var answers: List<Answer> by mutableStateOf(emptyList())
    private set

  var lives: Int by mutableStateOf(config.lives)
    private set

  var recalled: Int by mutableStateOf(0)
    private set

  var asked: Int by mutableStateOf(0)
    private set

  var best: Int by mutableStateOf(records.best())
    private set

  var record: Record? by mutableStateOf(null)
    private set

  var round: Int by mutableStateOf(1)
    private set

  private var next: Deferred<Result<RoundGame>> = fetch()

  init {
    startRound()
  }

  fun watched() {
    if (stage !is Stage.Watching) return
    stage = if (words.isEmpty()) endRound() else Stage.Solving(0)
  }

  suspend fun submit(letters: List<String>): Submission? {
    val s = solving() ?: return null
    val word = words[s.index]
    val verdict = when {
      letters == word.tiles -> Verdict.HIT
      isWord(letters) -> Verdict.NOT_PLAYED
      config.single -> Verdict.INVALID
      else -> return Submission.Rejected
    }
    if (solving() != s) return null
    resolve(s, verdict)
    return Submission.Resolved(verdict)
  }

  fun forget() {
    solving()?.let { resolve(it, Verdict.FORGOTTEN) }
  }

  fun next() {
    val s = stage as? Stage.Solving ?: return
    if (s.verdict == null) return
    stage = when {
      lives == 0 -> endSeries()
      s.index + 1 < words.size -> Stage.Solving(s.index + 1)
      else -> endRound()
    }
  }

  fun retry() {
    if (stage is Stage.Failed) startRound()
  }

  fun nextRound() {
    if (stage != Stage.RoundDone) return
    round++
    startRound()
  }

  fun restart() {
    lives = config.lives
    recalled = 0
    asked = 0
    round = 1
    best = records.best()
    record = null
    startRound()
  }

  private fun solving(): Stage.Solving? = (stage as? Stage.Solving)?.takeIf { it.verdict == null }

  private fun resolve(s: Stage.Solving, verdict: Verdict) {
    val hit = verdict == Verdict.HIT
    answers = answers + Answer(words[s.index], hit)
    asked++
    if (hit) recalled++ else lives--
    stage = s.copy(verdict = verdict)
  }

  private fun endRound(): Stage = if (lives == 0) endSeries() else Stage.RoundDone

  private fun endSeries(): Stage {
    record = records.submit(recalled)
    return Stage.SeriesDone
  }

  private suspend fun isWord(letters: List<String>): Boolean =
    withContext(Dispatchers.Default) { source.isWord(dictionaryText(letters)) }

  private fun startRound() {
    stage = Stage.Loading
    answers = emptyList()
    val pending = next
    next = fetch()
    scope.launch { pending.await().fold(::watch) { stage = Stage.Failed(it.message ?: it.toString()) } }
  }

  private fun watch(round: RoundGame) {
    words = round.words
    stage = Stage.Watching(round.game)
  }

  private fun fetch(): Deferred<Result<RoundGame>> =
    scope.async(Dispatchers.Default) { runCatching { roundGame() } }

  private suspend fun roundGame(): RoundGame {
    val game = source.game()
    return RoundGame(game, Words.pick(game, config.wordsPerGame, random, source::isWord))
  }
}
