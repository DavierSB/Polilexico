package app.lexico.ui.common

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Barra superior de una partida: salir (la partida queda guardada), abandonar (solo si sigue en
 * juego), pausar (en las partidas con tiempo), cambiar el tema, la tabla de movidas y la bolsita
 * con las fichas que quedan.
 */
@Composable
fun GameBar(
  bag: Int,
  onExit: () -> Unit,
  onMoves: () -> Unit,
  onBag: () -> Unit,
  onResign: (() -> Unit)?,
  onPause: (() -> Unit)? = null,
  onTheme: (() -> Unit)? = null,
) {
  Row(Modifier.fillMaxWidth().height(36.dp), verticalAlignment = Alignment.CenterVertically) {
    TextButton(contentPadding = Compact, onClick = onExit) { Text("‹ Salir") }
    if (onResign != null) TextButton(contentPadding = Compact, onClick = onResign) { Text("Abandonar") }
    Spacer(Modifier.weight(1f))
    if (onPause != null) PauseButton(onPause)
    if (onTheme != null) TextButton(contentPadding = Compact, onClick = onTheme) { Text("Tema") }
    TextButton(contentPadding = Compact, onClick = onMoves) { Text("Movidas") }
    BagIcon(bag, onBag)
  }
}

/** Cabecera de las pantallas que no son partidas: volver, el titulo y, a la derecha, `extra`. */
@Composable
fun Header(title: String, onBack: () -> Unit, extra: @Composable () -> Unit = {}) {
  Row(verticalAlignment = Alignment.CenterVertically) {
    TextButton(contentPadding = Compact, onClick = onBack) { Text("‹ Volver") }
    Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
    extra()
  }
}
