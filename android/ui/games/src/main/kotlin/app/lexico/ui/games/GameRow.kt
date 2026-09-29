package app.lexico.ui.games

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Una partida en una lista: titulo y detalle a la izquierda, `trailing` a la derecha. Tocarla la abre. */
@Composable
internal fun GameRow(title: String, detail: String, onClick: () -> Unit, trailing: @Composable () -> Unit = {}) {
  Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
    Column(Modifier.weight(1f)) {
      Text(title, fontWeight = FontWeight.Bold)
      Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    trailing()
  }
  HorizontalDivider()
}

/** El aviso de una lista vacia. */
@Composable
internal fun EmptyList(text: String) {
  Text(text, Modifier.padding(top = 12.dp), style = MaterialTheme.typography.bodyMedium)
}
