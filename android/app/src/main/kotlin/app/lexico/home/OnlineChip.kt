package app.lexico.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.lexico.online.Online
import app.lexico.online.OnlineClient
import kotlinx.coroutines.delay

@Composable
internal fun rememberPresence(): Int? {
  val online = Online.of(LocalContext.current)
  val client by online.current.collectAsState()
  return client?.let { liveCount(it) } ?: polledCount(online)
}

@Composable
private fun liveCount(client: OnlineClient): Int? {
  val connected by client.connected.collectAsState()
  val lobby by client.lobby.collectAsState()
  return if (connected) lobby.people.size + 1 else null
}

@Composable
private fun polledCount(online: Online): Int? {
  val count by produceState<Int?>(null) {
    while (true) {
      value = online.api.presence().getOrNull()
      delay(PRESENCE_REFRESH_MS)
    }
  }
  return count
}

@Composable
internal fun OnlineChip(count: Int?, compact: Boolean, onClick: () -> Unit) {
  AssistChip(onClick, label = { ChipLabel(count, compact) }, leadingIcon = { StatusDot(count != null) })
}

@Composable
private fun ChipLabel(count: Int?, compact: Boolean) {
  val text = when {
    compact -> count?.toString() ?: "En línea"
    count == null -> "En línea"
    else -> "En línea · $count"
  }
  Text(text, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
}

@Composable
private fun StatusDot(reachable: Boolean) {
  Box(Modifier.size(8.dp).background(if (reachable) ONLINE_GREEN else Color.Gray, CircleShape))
}

private val ONLINE_GREEN = Color(0xFF2E9E5B)

private const val PRESENCE_REFRESH_MS = 60_000L
