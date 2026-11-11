package app.lexico.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import app.lexico.game.Lexico
import app.lexico.game.storage.Mode
import app.lexico.home.rememberPresence
import app.lexico.menu.Settings
import app.lexico.online.Online
import app.lexico.ui.common.NoProfileCard
import app.lexico.ui.common.OnlineTab
import app.lexico.ui.common.onlineLabel

private val chosenOnline = mutableMapOf<Mode, Boolean>()

@Composable
internal fun rememberOnlineTab(mode: Mode, lexico: Lexico, settings: Settings, nav: Navigator): OnlineTab {
  val online = rememberOnline()
  val client = rememberClient(lexico)
  return remember {
    val preferred = client.hasOwnGame(mode == Mode.DUPLICATE) || chosenOnline[mode] == true
    OnlineTab(preferred, label = { onlineLabel(rememberPresence()) }, onChosen = { chosenOnline[mode] = it }) {
      OnlinePanelRoute(mode, online, lexico, settings, nav)
    }
  }
}

@Composable
private fun OnlinePanelRoute(mode: Mode, online: Online, lexico: Lexico, settings: Settings, nav: Navigator) {
  val session by online.account.session.collectAsState()
  val current = session ?: return NoProfileCard(rememberPresence()) { nav.go(Screen.OnlineAccount) }
  OnlinePanelHost(current, mode, online, lexico, settings, nav)
}
