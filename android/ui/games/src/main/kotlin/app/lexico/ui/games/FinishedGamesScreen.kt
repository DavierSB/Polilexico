package app.lexico.ui.games

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import app.lexico.ui.common.Header

private val Win = Color(0xFF66BB6A)

/**
 * Una partida terminada: "Clásica contra HastyBot", "27 sep 2026, 23:10", el marcador ("421 – 485")
 * y si la ganaste (null = empate).
 */
data class FinishedItem(val key: String, val title: String, val detail: String, val score: String, val won: Boolean?)

/** Las partidas terminadas de una carpeta, la mas reciente primero: tocar una la abre para revisarla. */
@Composable
fun FinishedGamesScreen(folder: GameFolder, items: List<FinishedItem>, onBack: () -> Unit, onOpen: (String) -> Unit) {
  Column(Modifier.fillMaxSize()) {
    Header(folder.title, onBack)
    if (items.isEmpty()) EmptyList("Todavía no has terminado ninguna partida de este tipo.")
    LazyColumn {
      items(items, key = { it.key }) { item -> GameRow(item.title, item.detail, { onOpen(item.key) }) { Score(item) } }
    }
  }
}

/** El marcador, en verde si ganaste y en rojo si perdiste. */
@Composable
private fun Score(item: FinishedItem) {
  val color = when (item.won) {
    true -> Win
    false -> MaterialTheme.colorScheme.error
    null -> MaterialTheme.colorScheme.onSurface
  }
  Text(item.score, color = color, fontWeight = FontWeight.Bold)
}
