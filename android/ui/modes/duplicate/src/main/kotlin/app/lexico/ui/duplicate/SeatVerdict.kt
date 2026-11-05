package app.lexico.ui.duplicate

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.sp
import app.lexico.ui.common.VerdictDialog
import app.lexico.ui.common.VerdictLine
import app.lexico.ui.common.VerdictTone

@Composable
internal fun SeatVerdictOnce(view: SeatView) {
  var seen by remember { mutableStateOf(view.phase == SeatPhase.FINISHED) }
  var open by remember { mutableStateOf(false) }
  LaunchedEffect(view.phase) {
    if (view.phase == SeatPhase.FINISHED && !seen) open = true
    seen = view.phase == SeatPhase.FINISHED
  }
  if (open) SeatVerdict(view) { open = false }
}

@Composable
private fun SeatVerdict(view: SeatView, close: () -> Unit) {
  val players = view.standings.filter { it.name != MASTER_NAME }
  val top = players.firstOrNull() ?: return
  val winners = players.filter { it.total == top.total }
  VerdictDialog(title(view, winners), tone(view, winners), close, titleSize = 40.sp) {
    VerdictLine("${top.total} pts", 34.sp, bold = true)
    masterShare(view, top.total)?.let { VerdictLine(it, 14.sp, faded = true) }
    placeOf(view, players)?.let { VerdictLine(it) }
  }
}

private fun title(view: SeatView, winners: List<SeatEntry>): String = when {
  winners.size != 1 -> "Empate"
  !view.watching && winners[0].name == view.me -> "¡Ganaste!"
  else -> "${winners[0].name} ganó"
}

private fun tone(view: SeatView, winners: List<SeatEntry>): VerdictTone = when {
  winners.size != 1 -> VerdictTone.NEUTRAL
  view.watching || winners[0].name == view.me -> VerdictTone.WIN
  else -> VerdictTone.NEUTRAL
}

private fun masterShare(view: SeatView, total: Int): String? {
  val master = view.standings.find { it.name == MASTER_NAME }?.total?.takeIf { it > 0 } ?: return null
  return "%.1f %% del Máster".format(total * 100f / master)
}

private fun placeOf(view: SeatView, players: List<SeatEntry>): String? {
  if (view.watching) return null
  val mine = players.indexOfFirst { it.name == view.me }.takeIf { it > 0 } ?: return null
  return "Quedaste ${mine + 1}.º con ${players[mine].total} pts"
}
