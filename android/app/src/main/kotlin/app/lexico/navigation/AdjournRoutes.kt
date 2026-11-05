package app.lexico.navigation

import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import app.lexico.online.OnlineClient
import app.lexico.online.RoomInfo
import app.lexico.ui.common.AdjournAskDialog
import app.lexico.ui.common.AdjournConfirm
import app.lexico.ui.common.AdjudicateConfirm
import app.lexico.ui.common.RivalAwayDialog
import kotlinx.coroutines.delay

private const val AWAY_MS = 3 * 60_000L
private const val ASK_AGAIN_MS = 2 * 60_000L

private enum class AwayStep { CHOOSE, ADJOURN, ADJUDICATE }

internal fun rivalOf(info: RoomInfo): String = info.players.firstOrNull { it != info.me } ?: "Tu rival"

@Composable
internal fun AdjournDialogs(client: OnlineClient, info: RoomInfo, over: Boolean, confirming: Boolean, onConfirmed: () -> Unit) {
  val rivalOnline by client.rivalOnline.collectAsState()
  val ask by client.adjournAsk.collectAsState()
  if (confirming) AdjournConfirm(rivalOf(info), rivalOnline, onYes = { onConfirmed(); client.adjourn() }, onNo = onConfirmed)
  ask?.let { AdjournAskDialog(it, onAccept = { client.answerAdjourn(true) }, onDecline = { client.answerAdjourn(false) }) }
  if (!over) RivalAwayPrompt(client, rivalOf(info))
}

@Composable
private fun RivalAwayPrompt(client: OnlineClient, rival: String) {
  val since by client.rivalAwaySince.collectAsState()
  var waits by remember { mutableIntStateOf(0) }
  var step by remember { mutableStateOf<AwayStep?>(null) }
  LaunchedEffect(since, waits) {
    step = null
    val start = since ?: return@LaunchedEffect
    delay((start + AWAY_MS + waits * ASK_AGAIN_MS - SystemClock.elapsedRealtime()).coerceAtLeast(0))
    step = AwayStep.CHOOSE
  }
  val close: () -> Unit = { step = null; waits += 1 }
  when (step) {
    AwayStep.CHOOSE -> RivalAwayDialog(rival, { step = AwayStep.ADJOURN }, { step = AwayStep.ADJUDICATE }, close)
    AwayStep.ADJOURN -> AdjournConfirm(rival, rivalOnline = false, onYes = { step = null; client.adjourn() }, onNo = close)
    AwayStep.ADJUDICATE -> AdjudicateConfirm(onYes = { step = null; client.adjudicate() }, onNo = close)
    null -> Unit
  }
}
