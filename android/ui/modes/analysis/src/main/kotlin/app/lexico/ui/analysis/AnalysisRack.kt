package app.lexico.ui.analysis

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.lexico.ui.common.Compact
import app.lexico.ui.board.BoardStyle
import app.lexico.ui.board.RackTile

@Composable
fun AnalysisRack(
  rack: List<String>,
  style: BoardStyle,
  analyzing: Boolean,
  onRemove: (Int) -> Unit,
  onAnalyze: () -> Unit,
) {
  Row(Modifier.fillMaxWidth().height(40.dp), verticalAlignment = Alignment.CenterVertically) {
    Text("Atril ", style = MaterialTheme.typography.labelLarge)
    RemovableTiles(rack, style, onRemove)
    Button(enabled = rack.isNotEmpty() && !analyzing, contentPadding = Compact, onClick = onAnalyze) {
      Text(if (analyzing) "…" else "Analizar")
    }
  }
}

@Composable
private fun RowScope.RemovableTiles(rack: List<String>, style: BoardStyle, onRemove: (Int) -> Unit) {
  Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
    rack.forEachIndexed { i, l -> Box(Modifier.clickable { onRemove(i) }) { RackTile(l, style, size = 34.dp) } }
  }
}
