package app.lexico.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import app.lexico.online.Online
import app.lexico.online.OnlineClient
import app.lexico.online.RoomInfo
import app.lexico.online.Seek
import app.lexico.ui.common.ConfirmDialog
import app.lexico.ui.common.InviteCard

@Composable
internal fun IncomingInvites(nav: Navigator, refuse: Boolean, modifier: Modifier = Modifier) {
  val online = rememberOnline()
  val client by online.current.collectAsState()
  client?.let { Invites(online, it, nav, refuse, modifier) }
}

@Composable
private fun Invites(online: Online, client: OnlineClient, nav: Navigator, refuse: Boolean, modifier: Modifier) {
  val lobby by client.lobby.collectAsState()
  val room by client.room.collectAsState()
  val invite = lobby.invites.firstOrNull() ?: return
  if (refuse && !invite.resume) return LaunchedEffect(invite.id) { client.decline(invite) }
  if (!canInterrupt(nav.current, room)) return
  ProvidePhotos(online, lobby) { Invite(client, invite, nav, modifier) }
}

@Composable
private fun Invite(client: OnlineClient, invite: Seek, nav: Navigator, modifier: Modifier) {
  var confirming by remember(invite.id) { mutableStateOf(false) }
  val accept = { confirming = false; client.accept(invite); nav.go(Screen.OnlinePlay) }
  InviteCard(seekItem(invite), onAccept = { if (nav.current.isGame) confirming = true else accept() }, onDecline = { client.decline(invite) }, modifier)
  if (confirming) ConfirmDialog(LEAVE_GAME, yes = "Aceptar", no = "Cancelar", onYes = accept) { confirming = false }
}

private fun canInterrupt(screen: Screen, room: RoomInfo?): Boolean =
  screen != Screen.OnlinePlay && !screen.isNearby && (room == null || room.watching)

private val Screen.isNearby: Boolean get() = this is Screen.WifiHost || this is Screen.WifiJoin || this is Screen.DuplicateWifiHost ||
  this is Screen.DuplicateWifiJoin || this is Screen.EndgameWifiHost || this is Screen.EndgameWifiJoin || this is Screen.RaceHost || this is Screen.RaceJoin

private const val LEAVE_GAME = "¿Dejar esta partida y aceptar el reto?"
