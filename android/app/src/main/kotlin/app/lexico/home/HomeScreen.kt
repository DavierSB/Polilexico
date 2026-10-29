package app.lexico.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.lexico.navigation.Screen
import app.lexico.ui.board.BoardStyle
import app.lexico.ui.common.Header
import app.lexico.ui.common.InfoButton

@Composable
fun HomeScreen(
  menu: Screen, style: BoardStyle, counts: MenuCounts, demoGame: suspend () -> List<String>, hasUpdate: Boolean, onMenu: () -> Unit,
  back: () -> Unit, go: (Screen) -> Unit,
) {
  Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    if (menu == Screen.Home) HomeTopBar(hasUpdate, onMenu) else Header(menuTitle(menu), back) { MenuInfo(menu) }
    DemoBoard(demoGame, style, Modifier.weight(1f, fill = false))
    menuButtons(menu, counts).forEach { MenuButton(it, go) }
  }
}

@Composable
private fun MenuInfo(menu: Screen) {
  menuInfo(menu)?.let { (title, text) -> InfoButton(title, text) }
}

@Composable
private fun MenuButton(entry: MenuEntry, go: (Screen) -> Unit) {
  val open = { go(entry.screen()) }
  if (entry.main) Button(onClick = open, Modifier.fillMaxWidth()) { Text(entry.name) }
  else OutlinedButton(onClick = open, Modifier.fillMaxWidth(), enabled = entry.enabled) { Text(entry.name) }
}

@Composable
private fun HomeTopBar(hasUpdate: Boolean, onMenu: () -> Unit) {
  Row(verticalAlignment = Alignment.CenterVertically) {
    DrawerButton(hasUpdate, onMenu)
    Logo(Modifier.padding(start = 4.dp), scale = 0.8f)
  }
}

@Composable
private fun DrawerButton(hasUpdate: Boolean, onMenu: () -> Unit) {
  Box {
    TextButton(onClick = onMenu) { Text("☰", fontSize = 22.sp) }
    if (hasUpdate) Badge(Modifier.align(Alignment.TopEnd).padding(top = 8.dp, end = 8.dp), containerColor = MaterialTheme.colorScheme.primary)
  }
}
