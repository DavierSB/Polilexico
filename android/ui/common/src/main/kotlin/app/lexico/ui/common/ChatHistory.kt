package app.lexico.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ChatRecord(val at: Long, val who: String, val country: String, val text: String, val mine: Boolean, val room: Boolean)

@Composable
fun ChatHistoryDialog(records: List<ChatRecord>, onClose: () -> Unit) {
  AlertDialog(
    onDismissRequest = onClose, title = { Text("Historial") },
    text = { if (records.isEmpty()) EmptyHistory() else HistoryList(records) },
    confirmButton = { TextButton(onClick = onClose) { Text("Cerrar") } },
  )
}

@Composable
private fun EmptyHistory() {
  Column(Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
    Text(CHAT_GLYPH, fontSize = 40.sp)
    Text("Aún no hay mensajes", color = MaterialTheme.colorScheme.onSurfaceVariant)
  }
}

@Composable
private fun HistoryList(records: List<ChatRecord>) {
  val state = rememberLazyListState(initialFirstVisibleItemIndex = records.lastIndex)
  LazyColumn(Modifier.heightIn(max = 460.dp), state, verticalArrangement = Arrangement.spacedBy(6.dp)) {
    items(records) { RecordBubble(it) }
  }
}

@Composable
private fun RecordBubble(r: ChatRecord) {
  val colors = MaterialTheme.colorScheme
  val fill = if (r.mine) colors.primaryContainer else colors.surfaceVariant
  Row(
    Modifier.fillMaxWidth().background(fill, RoundedCornerShape(14.dp)).padding(horizontal = 10.dp, vertical = 8.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    if (r.room && r.mine) RoomAvatar(32.dp) else Box(Modifier.opensProfile(r.who, r.country)) { PersonAvatar(r.who, r.country, size = 32.dp) }
    Column(Modifier.weight(1f)) {
      RecordHeader(r)
      Text(r.text)
    }
  }
}

@Composable
private fun RecordHeader(r: ChatRecord) {
  Row(verticalAlignment = Alignment.CenterVertically) {
    Text(route(r), Modifier.weight(1f), fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    Text(stamp(r.at), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
  }
}

private fun route(r: ChatRecord): String {
  val other = if (r.country.isEmpty()) r.who else "${r.who} ${flagOf(r.country)}"
  return when {
    r.mine && r.room -> "Tú → $WHOLE_ROOM"
    r.mine -> "Tú → $other"
    r.room -> "$other → $WHOLE_ROOM"
    else -> "$other → Tú"
  }
}

private fun stamp(at: Long): String = SimpleDateFormat("d MMM HH:mm", Locale.forLanguageTag("es")).format(Date(at))

@Composable
internal fun RoomRow(onClick: () -> Unit) {
  Row(
    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable(onClick = onClick)
      .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(14.dp)).padding(horizontal = 10.dp, vertical = 8.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically,
  ) {
    RoomAvatar(40.dp)
    Text(WHOLE_ROOM, fontWeight = FontWeight.Bold)
  }
}

@Composable
private fun RoomAvatar(size: Dp) {
  Box(Modifier.size(size).clip(CircleShape).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
    Text("👥", fontSize = (size.value * 0.45f).sp)
  }
}
