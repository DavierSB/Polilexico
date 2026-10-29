package app.lexico.ui.lists

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random

sealed interface TestStage {
  data object Loading : TestStage

  data class Solving(val hand: Hand, val found: List<List<String>> = emptyList()) : TestStage

  data class Revealed(val hand: Hand, val found: List<List<String>>) : TestStage

  data object Completed : TestStage

  data object NoHands : TestStage
}

enum class TestReply { LEARNED, ALREADY, OUTSIDE, REPEATED, INVALID }

@Stable
class TestSession(
  start: WordList,
  val lexicon: Lexicon,
  private val source: HandSource,
  private val book: ListBook,
  private val scope: CoroutineScope,
  private val random: Random = Random.Default,
) {
  var list: WordList by mutableStateOf(start)
    private set
  var stage: TestStage by mutableStateOf(TestStage.Loading)
    private set
  var hand by mutableIntStateOf(0)
    private set

  private val filter = start.spec.filter()
  private val skipped = mutableSetOf<String>()

  init {
    startHand()
  }

  fun submit(letters: List<String>): TestReply? {
    val s = stage as? TestStage.Solving ?: return null
    return when (letters) {
      !in s.hand.answers -> TestReply.INVALID
      in s.found -> TestReply.REPEATED
      else -> hit(s, letters)
    }
  }

  fun pass() {
    val s = stage as? TestStage.Solving ?: return
    stage = TestStage.Revealed(s.hand, s.found)
  }

  fun next() {
    if (stage is TestStage.Revealed) startHand()
  }

  fun restart() {
    save(list.restart())
    skipped.clear()
    startHand()
  }

  private fun hit(s: TestStage.Solving, letters: List<String>): TestReply {
    val before = list.learned.size
    val keys = lexicon.unitsOf(WordKeys.of(letters)).map { it.key }
    save(list.learn(keys))
    val found = s.found + listOf(letters)
    stage = if (found.size == s.hand.answers.size) TestStage.Revealed(s.hand, found) else s.copy(found = found)
    return when {
      list.learned.size > before -> TestReply.LEARNED
      keys.any(list::has) -> TestReply.ALREADY
      else -> TestReply.OUTSIDE
    }
  }

  private fun save(next: WordList) {
    list = next
    book.save(next)
  }

  private fun startHand() {
    if (list.isComplete) {
      stage = TestStage.Completed
      return
    }
    stage = TestStage.Loading
    scope.launch { pose(withContext(Dispatchers.Default) { nextHand() }) }
  }

  private fun pose(next: Hand?) {
    if (next == null) {
      stage = TestStage.NoHands
      return
    }
    hand++
    stage = TestStage.Solving(next)
  }

  private suspend fun nextHand(): Hand? {
    lexicon.preload()
    for (key in list.pending.filter { it !in skipped }.shuffled(random)) {
      val unit = lexicon.find(key)
      val next = unit?.let { Hands.of(it, lexicon, filter, source, random) }
      if (next != null) return next
      skipped += key
    }
    return null
  }
}
