package app.lexico.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
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

@Composable
fun HomeScreen(
  style: BoardStyle, inProgress: Int, demoGame: suspend () -> List<String>, onMenu: () -> Unit, go: (Screen) -> Unit,
) {
  Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    HomeTopBar(onMenu)
    DemoBoard(demoGame, style, Modifier.weight(1f, fill = false))
    MODES.forEach { (name, screen) -> Button(onClick = { go(screen) }, Modifier.fillMaxWidth()) { Text(name) } }
    SavedGamesButtons(inProgress, go)
  }
}

@Composable
private fun HomeTopBar(onMenu: () -> Unit) {
  Row(verticalAlignment = Alignment.CenterVertically) {
    TextButton(onClick = onMenu) { Text("☰", fontSize = 22.sp) }
    Logo(Modifier.padding(start = 4.dp), scale = 0.8f)
  }
}

@Composable
private fun SavedGamesButtons(inProgress: Int, go: (Screen) -> Unit) {
  OutlinedButton(onClick = { go(Screen.InProgress) }, Modifier.fillMaxWidth(), enabled = inProgress > 0) {
    Text("Partidas en curso ($inProgress)")
  }
  OutlinedButton(onClick = { go(Screen.Finished) }, Modifier.fillMaxWidth()) { Text("Mis partidas") }
}

private val MODES = listOf(
  "Clásica" to Screen.NewClassic,
  "Duplicada" to Screen.NewDuplicate,
  "Minijuegos" to Screen.Minigames,
  "Analizador" to Screen.Analyzer,
)

