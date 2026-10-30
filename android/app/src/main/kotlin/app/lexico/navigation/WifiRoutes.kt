package app.lexico.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import app.lexico.game.Lexico
import app.lexico.game.modes.classic.ClassicGame
import app.lexico.menu.ThemeState
import app.lexico.modes.classic.toSetup
import app.lexico.ui.common.ConnectingScreen
import app.lexico.ui.common.RoomItem
import app.lexico.ui.common.RoomsScreen
import app.lexico.ui.common.WaitingRoomScreen
import app.lexico.wifi.Room
import app.lexico.wifi.RoomFinder
import app.lexico.wifi.RoomKind
import app.lexico.wifi.WifiGuest
import app.lexico.wifi.WifiHost

@Composable
internal fun WifiHostRoute(screen: Screen.WifiHost, lexico: Lexico, nav: Navigator, themes: ThemeState) =
  HostRoute(screen, screen.name, RoomKind.CLASSIC, lexico, nav, themes) { guest ->
    lexico.newWifiHost(screen.config.copy(players = screen.name to guest).toSetup())
  }

@Composable
internal fun EndgameHostRoute(screen: Screen.EndgameWifiHost, lexico: Lexico, nav: Navigator, themes: ThemeState) =
  HostRoute(screen, screen.name, RoomKind.ENDGAME, lexico, nav, themes) { guest ->
    lexico.newWifiEndgame(screen.config.copy(players = screen.name to guest).toSetup())
  }

@Composable
internal fun WifiJoinRoute(screen: Screen.WifiJoin, lexico: Lexico, nav: Navigator, themes: ThemeState) =
  JoinRoute(screen.name, RoomKind.CLASSIC, lexico, nav, themes)

@Composable
internal fun EndgameJoinRoute(screen: Screen.EndgameWifiJoin, lexico: Lexico, nav: Navigator, themes: ThemeState) =
  JoinRoute(screen.name, RoomKind.ENDGAME, lexico, nav, themes)

@Composable
private fun HostRoute(
  key: Screen, name: String, kind: RoomKind, lexico: Lexico, nav: Navigator, themes: ThemeState, start: suspend (guest: String) -> ClassicGame,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val host = remember(key) { WifiHost(context, name, scope, kind, start) }
  val error = remember(host) { host.open() }
  DisposableEffect(host) { onDispose { host.close() } }
  val game by host.game.collectAsState()
  val notice by host.notice.collectAsState()
  game?.let { ClassicPlay(it, nav, themes, lexico.finishedGames, notice, handOff = false) }
    ?: WaitingRoomScreen(name, error, nav::back)
}

@Composable
private fun JoinRoute(name: String, kind: RoomKind, lexico: Lexico, nav: Navigator, themes: ThemeState) {
  var room by remember { mutableStateOf<Room?>(null) }
  var refusal by remember { mutableStateOf<String?>(null) }
  val chosen = room
  if (chosen == null) FindRooms(kind, refusal, nav) { room = it; refusal = null }
  else GuestRoute(name, chosen, lexico, nav, themes) { refusal = it; room = null }
}

@Composable
internal fun FindRooms(kind: RoomKind, refusal: String?, nav: Navigator, onJoin: (Room) -> Unit) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val finder = remember { RoomFinder(context, scope, kind) }
  DisposableEffect(finder) { finder.start(); onDispose { finder.stop() } }
  val rooms by finder.rooms.collectAsState()
  RoomsScreen(rooms.map { RoomItem(it.name, it.address) }, refusal, { onJoin(Room(it.name, it.address, kind.port)) }, nav::back)
}

@Composable
private fun GuestRoute(name: String, room: Room, lexico: Lexico, nav: Navigator, themes: ThemeState, onRefused: (String) -> Unit) {
  val scope = rememberCoroutineScope()
  val guest = remember(room) { WifiGuest(name, room, scope) { save, paused, remote -> lexico.joinWifi(save, paused, remote) } }
  DisposableEffect(guest) { guest.open(); onDispose { guest.close() } }
  val game by guest.game.collectAsState()
  val notice by guest.notice.collectAsState()
  val refusal by guest.refusal.collectAsState()
  LaunchedEffect(refusal) { refusal?.let(onRefused) }
  game?.let { ClassicPlay(it, nav, themes, lexico.finishedGames, notice, handOff = false) }
    ?: ConnectingScreen(room.name, notice) { onRefused("") }
}
