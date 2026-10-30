package app.lexico.game.race

import app.lexico.game.modes.recall.ScoredPlacement
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val WATCH_MARGIN_MS = 3_000L

class Race(
  private val setup: RaceSetup,
  players: List<String>,
  private val scope: CoroutineScope,
  private val nextGame: (suspend () -> List<ScoredPlacement>)? = null,
  private val now: () -> Long = System::currentTimeMillis,
) {
  private val startedAt = now()
  private val entries = players.associateWith { Entry() }
  private var over = false
  private var round: RaceRound? = null
  private var roundEndsAt: Long? = null
  private var opening = false
  private val _view = MutableStateFlow(snapshot())
  val view: StateFlow<RaceView> = _view.asStateFlow()

  init {
    if (setup.timeMs > 0) scope.launch { delay(setup.timeMs); finish() }
    if (nextGame != null) openRound(1)
  }

  val players: Set<String> get() = entries.keys

  @Synchronized
  fun report(player: String, score: Int, points: Int, done: Boolean, lives: Int = 0) {
    val entry = entries[player] ?: return
    if (over) return
    if (score > entry.score) entry.reachedAt = now()
    entry.score = score
    entry.points = points
    entry.done = done || entry.ready
    entry.lives = lives
    settle()
  }

  @Synchronized
  fun setPresent(player: String, present: Boolean) {
    entries[player]?.present = present
    settle()
  }

  @Synchronized
  fun finishRound(player: String, number: Int) {
    if (player in entries) closeRound(number)
  }

  @Synchronized
  private fun closeRound(number: Int) {
    val current = round ?: return
    if (over || current.number != number || !current.open) return
    round = current.copy(open = false)
    roundEndsAt = null
    if (setup.rounds in 1..number) over = true
    settle()
  }

  @Synchronized
  fun ready(player: String) {
    entries[player]?.apply { ready = true; done = true }
    settle()
  }

  @Synchronized
  fun finish() {
    over = true
    publish()
  }

  private fun settle() {
    if (reachedGoal()) over = true
    if (everyoneDone()) endForAll()
    if (allReady()) openRound((round?.number ?: 0) + 1)
    publish()
  }

  private fun endForAll() {
    val current = round ?: return run { over = true }
    if (!current.open) return
    round = current.copy(open = false)
    roundEndsAt = null
  }

  private fun reachedGoal(): Boolean = setup.goal > 0 && entries.values.any { it.score >= setup.goal }

  private fun everyoneDone(): Boolean = playing().let { it.isNotEmpty() && it.all(Entry::done) }

  private fun allReady(): Boolean = !over && !opening && round?.open == false && playing().all(Entry::ready)

  private fun playing(): List<Entry> = entries.values.filter { it.present }

  private fun openRound(number: Int) {
    val game = nextGame ?: return
    opening = true
    scope.launch {
      val next = game()
      synchronized(this@Race) {
        opening = false
        entries.values.forEach { it.ready = false; it.done = false }
        round = RaceRound(number, open = true, next)
        roundEndsAt = closeLater(number, next.size)
        publish()
      }
    }
  }

  private fun closeLater(number: Int, plays: Int): Long? {
    if (setup.roundMs <= 0) return null
    val ms = plays * setup.intervalMs + WATCH_MARGIN_MS + setup.roundMs
    scope.launch { delay(ms); closeRound(number) }
    return now() + ms
  }

  private fun publish() {
    _view.value = snapshot()
  }

  private fun snapshot(): RaceView = RaceView(setup, over, remainingMs(), standings(), round?.copy(remainingMs = roundEndsAt?.let { maxOf(0, it - now()) }))

  private fun remainingMs(): Long? = setup.timeMs.takeIf { it > 0 }?.let { maxOf(0, it - (now() - startedAt)) }

  private fun standings(): List<Runner> = entries.entries
    .sortedWith(compareByDescending<Map.Entry<String, Entry>> { it.value.score }.thenByDescending { it.value.points }.thenBy { it.value.reachedAt })
    .map { (name, e) -> Runner(name, e.score, e.points, e.done, e.present, e.lives, e.ready) }

  private class Entry(
    var score: Int = 0, var points: Int = 0, var done: Boolean = false, var present: Boolean = true, var lives: Int = 0,
    var ready: Boolean = false, var reachedAt: Long = Long.MAX_VALUE,
  )
}
