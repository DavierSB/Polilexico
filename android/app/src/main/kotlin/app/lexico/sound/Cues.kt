package app.lexico.sound

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import app.lexico.game.Cue
import app.lexico.game.LiveGame
import app.lexico.ui.recall.RecallSession
import app.lexico.ui.recall.Stage
import app.lexico.ui.recall.Verdict
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.mapNotNull

val LocalCues = staticCompositionLocalOf<(Cue) -> Unit> { {} }

@Composable
fun PlayCues(game: LiveGame<*>, adjust: (Cue) -> Cue? = { it }) {
  val play = LocalCues.current
  LaunchedEffect(game) { game.cues.collect { cue -> adjust(cue)?.let(play) } }
}

@Composable
fun PlayCues(session: RecallSession) {
  val play = LocalCues.current
  LaunchedEffect(session) { verdicts(session).mapNotNull(::cueOf).collect(play) }
  LaunchedEffect(session) { snapshotFlow { session.rejections }.drop(1).collect { play(Cue.WRONG) } }
  CelebrateRecord(recordBroken(session.recalled, session.best, session.record?.isNew == true))
}

@Composable
fun CelebrateRecord(broken: Boolean) {
  val play = LocalCues.current
  LaunchedEffect(broken) { if (broken) play(Cue.CELEBRATION) }
}

fun recordBroken(solved: Int, best: Int, isNew: Boolean): Boolean = best in 1 until solved || isNew

private fun verdicts(session: RecallSession) = snapshotFlow { (session.stage as? Stage.Solving)?.verdict }.drop(1)

private fun cueOf(verdict: Verdict?): Cue? = when (verdict) {
  Verdict.HIT -> Cue.CORRECT
  Verdict.INVALID, Verdict.NOT_PLAYED -> Cue.WRONG
  else -> null
}
