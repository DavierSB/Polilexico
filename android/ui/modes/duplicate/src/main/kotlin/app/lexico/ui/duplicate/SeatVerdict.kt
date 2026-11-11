package app.lexico.ui.duplicate

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.lexico.ui.common.VerdictDialog
import app.lexico.ui.common.VerdictLine
import app.lexico.ui.common.VerdictTone

@Composable
internal fun SeatVerdictOnce(view: SeatView, onLobby: (() -> Unit)? = null) {
  var seen by remember { mutableStateOf(view.phase == SeatPhase.FINISHED) }
  var open by remember { mutableStateOf(false) }
  LaunchedEffect(view.phase) {
    if (view.phase == SeatPhase.FINISHED && !seen) open = true
    seen = view.phase == SeatPhase.FINISHED
  }
  if (!open) return
  if (onLobby != null) FinalStandings(view, onLobby) { open = false } else SeatVerdict(view) { open = false }
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

@Composable
private fun FinalStandings(view: SeatView, onLobby: () -> Unit, close: () -> Unit) {
  val players = view.standings.filter { it.name != MASTER_NAME }
  val top = players.firstOrNull() ?: return
  val winners = players.filter { it.total == top.total }
  VerdictDialog(title(view, winners), tone(view, winners), close, titleSize = 40.sp) {
    Column(Modifier.padding(horizontal = 24.dp).widthIn(max = 460.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
      LeaderboardTable(view, maxHeight = FINAL_HEIGHT, me = view.me.takeUnless { view.watching })
      Button(onClick = { close(); onLobby() }, Modifier.fillMaxWidth()) { Text("Volver al lobby") }
    }
  }
}

private const val FINAL_HEIGHT = 380

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
