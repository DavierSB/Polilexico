package app.lexico.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class RoomItem(val name: String, val address: String)

@Composable
fun WaitingRoomScreen(name: String, error: String?, onBack: () -> Unit) {
  FitColumn(14.dp) {
    Header("Sala de $name", onBack)
    if (error != null) return@FitColumn Text(error, color = MaterialTheme.colorScheme.error)
    PlayerGrid(listOf(name), host = name, me = name, waiting = "Esperando rival…")
    HostHint()
  }
}

@Composable
fun HostLobbyScreen(name: String, players: List<String>, error: String?, onStart: () -> Unit, onBack: () -> Unit) {
  FitColumn(12.dp) {
    Header("Sala de $name", onBack)
    if (error != null) return@FitColumn Text(error, color = MaterialTheme.colorScheme.error)
    PlayerGrid(players, host = name, me = name, waiting = "Esperando…")
    HostHint()
    Button(modifier = Modifier.fillMaxWidth().height(52.dp), enabled = players.size > 1, onClick = onStart) {
      Text("Empezar la partida", fontWeight = FontWeight.Bold)
    }
  }
}

@Composable
fun GuestLobbyScreen(room: String, players: List<String>, notice: String?, me: String, onBack: () -> Unit) {
  FitColumn(12.dp) {
    Header("Sala de $room", onBack)
    if (players.isEmpty()) return@FitColumn Waiting(notice ?: "Conectando…")
    PlayerGrid(players, host = room, me = me, waiting = null)
    StatusCard(notice ?: "$room empezará la partida en breve")
  }
}

@Composable
private fun PlayerGrid(players: List<String>, host: String, me: String, waiting: String?) {
  SectionTitle("Jugadores · ${players.size}")
  val tiles: List<String?> = if (waiting != null) players + null else players
  tiles.chunked(2).forEach { row ->
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      row.forEach { player ->
        Box(Modifier.weight(1f)) { if (player == null) WaitingTile(waiting.orEmpty()) else PlayerTile(player, player == host, player == me) }
      }
      if (row.size == 1) Spacer(Modifier.weight(1f))
    }
  }
}

@Composable
private fun PlayerTile(name: String, host: Boolean, mine: Boolean) {
  val colors = MaterialTheme.colorScheme
  Row(
    Modifier.fillMaxWidth().height(TILE_HEIGHT).background(if (mine) colors.primaryContainer else colors.surfaceVariant, RoundedCornerShape(14.dp))
      .padding(horizontal = 10.dp),
    horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically,
  ) {
    PlayerAvatar(name, mine)
    Column {
      Text(name, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
      Text(if (host) "Crea la sala" else "Conectado", style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
    }
  }
}

@Composable
private fun WaitingTile(text: String) {
  val colors = MaterialTheme.colorScheme
  Row(
    Modifier.fillMaxWidth().height(TILE_HEIGHT).border(1.dp, colors.outlineVariant, RoundedCornerShape(14.dp)).padding(horizontal = 12.dp),
    horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically,
  ) {
    CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
    Text(text, color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
  }
}

@Composable
private fun HostHint() {
  Text(
    "Dile a los demás que se unan a tu wifi.", Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
  )
}

@Composable
private fun StatusCard(text: String) {
  Row(
    Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(16.dp)).padding(16.dp),
    horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically,
  ) {
    CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
    Text(text, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
  }
}

@Composable
internal fun PlayerAvatar(name: String, highlighted: Boolean, size: Dp = 40.dp) {
  val colors = MaterialTheme.colorScheme
  Box(Modifier.size(size).background(if (highlighted) colors.primary else colors.outlineVariant, CircleShape), contentAlignment = Alignment.Center) {
    Text(name.trim().take(1).uppercase(), fontSize = (size.value * 0.45f).sp, fontWeight = FontWeight.Black, color = if (highlighted) colors.onPrimary else colors.onSurface)
  }
}

private val TILE_HEIGHT = 60.dp

@Composable
fun RoomsScreen(rooms: List<RoomItem>, message: String?, onJoin: (RoomItem) -> Unit, onBack: () -> Unit) {
  FitColumn(10.dp) {
    Header("Unirse a una sala", onBack)
    message?.takeIf { it.isNotBlank() }?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    if (rooms.isEmpty()) Waiting("Buscando salas en esta red wifi…")
    rooms.forEach { RoomRow(it) { onJoin(it) } }
    ManualAddress(onJoin)
  }
}

@Composable
fun ConnectingScreen(room: String, notice: String?, onBack: () -> Unit) {
  FitColumn(14.dp) {
    Header("Sala de $room", onBack)
    Waiting(notice ?: "Conectando…")
  }
}

@Composable
private fun Waiting(text: String) {
  Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
    CircularProgressIndicator(Modifier.padding(4.dp))
    Text(text)
  }
}

@Composable
private fun RoomRow(room: RoomItem, join: () -> Unit) {
  Column(
    Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp)).clickable(onClick = join).padding(10.dp),
  ) {
    Text("Sala de ${room.name}", fontWeight = FontWeight.Bold)
  }
}

@Composable
private fun ManualAddress(onJoin: (RoomItem) -> Unit) {
  var address by remember { mutableStateOf("") }
  Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    OutlinedTextField(address, { address = it.trim() }, Modifier.weight(1f), singleLine = true, label = { Text("Dirección") })
    OutlinedButton(enabled = address.isNotEmpty(), onClick = { onJoin(RoomItem(address, address)) }) { Text("Conectar") }
  }
}
