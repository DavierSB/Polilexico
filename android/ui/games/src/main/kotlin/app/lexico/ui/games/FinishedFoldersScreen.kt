package app.lexico.ui.games

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.lexico.ui.common.Header

enum class GameFolder(val title: String) { CLASSIC("Clásica"), DUPLICATE("Duplicadas"), MINIGAMES("Minijuegos") }

@Composable
fun FinishedFoldersScreen(counts: Map<GameFolder, Int>, onBack: () -> Unit, onOpen: (GameFolder) -> Unit) {
  Column(Modifier.fillMaxSize()) {
    Header("Mis partidas", onBack)
    GameFolder.entries.forEach { folder -> FolderRow(folder, counts[folder] ?: 0) { onOpen(folder) } }
  }
}

@Composable
private fun FolderRow(folder: GameFolder, count: Int, onOpen: () -> Unit) {
  GameRow(folder.title, gameCount(count), onOpen) {
    Text("›", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
  }
}

private fun gameCount(count: Int): String = if (count == 1) "1 partida" else "$count partidas"
