package app.lexico.ui.games

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.lexico.ui.common.Header

/** Las carpetas de "Mis partidas", en el orden en que se muestran. Finales va en Minijuegos. */
enum class GameFolder(val title: String) { CLASSIC("Clásica"), DUPLICATE("Duplicadas"), MINIGAMES("Minijuegos") }

/** Las carpetas de las partidas terminadas, cada una con cuantas tiene: tocar una la abre. */
@Composable
fun FinishedFoldersScreen(counts: Map<GameFolder, Int>, onBack: () -> Unit, onOpen: (GameFolder) -> Unit) {
  Column(Modifier.fillMaxSize()) {
    Header("Mis partidas", onBack)
    GameFolder.entries.forEach { folder -> FolderRow(folder, counts[folder] ?: 0) { onOpen(folder) } }
  }
}

@Composable
private fun FolderRow(folder: GameFolder, count: Int, onOpen: () -> Unit) {
  GameRow(folder.title, partidas(count), onOpen) {
    Text("›", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
  }
}

private fun partidas(count: Int): String = if (count == 1) "1 partida" else "$count partidas"
