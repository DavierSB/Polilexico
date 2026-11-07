package app.lexico.sound

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import app.lexico.game.Cue
import app.lexico.game.Outcome
import app.lexico.game.modes.classic.ClassicState
import app.lexico.ui.duplicate.SeatPhase
import app.lexico.ui.duplicate.SeatView
import kotlinx.coroutines.delay

private const val CLASSIC_WARNING_MS = 60_000L
private const val ROUND_WARNING_MS = 30_000L
private const val ROUND_LAST_MS = 5_000L
private const val MASTER = "Máster"

@Composable
fun ClassicOnlineCues(state: ClassicState) {
  val play = LocalCues.current
  var last by remember { mutableStateOf<ClassicState?>(null) }
  LaunchedEffect(state) {
    last?.let { classicCues(it, state).forEach(play) }
    last = state
  }
}

private fun classicCues(before: ClassicState, now: ClassicState): List<Cue> = buildList {
  if (now.moves.size > before.moves.size && now.moves.last().byMe.not()) add(Cue.OPPONENT)
  if (before.result == null && now.result?.outcome == Outcome.WIN) add(Cue.CELEBRATION)
  val was = before.clocks?.myMs ?: return@buildList
  val left = now.clocks?.myMs ?: return@buildList
  if (!now.myTurn) return@buildList
  if (crossed(was, left, CLASSIC_WARNING_MS)) add(Cue.WARNING)
  if (crossed(was, left, 0)) add(Cue.TIME_UP)
}

private fun crossed(was: Long, now: Long, mark: Long): Boolean = was >= mark && now < mark

@Composable
fun SeatCues(view: SeatView) {
  if (view.watching) return
  RoundCountdownCues(view)
  RoundResultCues(view)
}

@Composable
private fun RoundCountdownCues(view: SeatView) {
  val play = LocalCues.current
  LaunchedEffect(view.round, view.phase, view.remainingMs) {
    val left = view.remainingMs ?: return@LaunchedEffect
    if (view.phase != SeatPhase.PLAYING) return@LaunchedEffect
    val start = System.currentTimeMillis()
    listOf(ROUND_WARNING_MS to Cue.WARNING, ROUND_LAST_MS to Cue.LAST_SECONDS).filter { left > it.first }.forEach { (mark, cue) ->
      delay(left - mark - (System.currentTimeMillis() - start))
      play(cue)
    }
  }
}

@Composable
private fun RoundResultCues(view: SeatView) {
  val play = LocalCues.current
  var phase by remember { mutableStateOf(view.phase) }
  LaunchedEffect(view.phase) {
    if (phase == SeatPhase.PLAYING && view.phase != SeatPhase.PLAYING) roundCue(view)?.let(play)
    if (phase != SeatPhase.FINISHED && view.phase == SeatPhase.FINISHED) play(if (iWon(view)) Cue.CELEBRATION else Cue.GAME_OVER)
    phase = view.phase
  }
}

private fun roundCue(view: SeatView): Cue? {
  val round = view.rounds.lastOrNull() ?: return null
  val master = round.entries.find { it.name == MASTER } ?: return null
  val mine = round.entries.find { it.name == view.me } ?: return null
  return if (mine.points > 0 && mine.points == master.points) Cue.CORRECT else Cue.MISS
}

private fun iWon(view: SeatView): Boolean {
  val players = view.standings.filter { it.name != MASTER }
  val top = players.firstOrNull() ?: return false
  return top.name == view.me && players.count { it.total == top.total } == 1
}
