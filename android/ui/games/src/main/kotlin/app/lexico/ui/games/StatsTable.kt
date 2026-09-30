package app.lexico.ui.games

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
internal fun StatsTable(rows: List<Pair<String, String>>) {
  Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))) {
    rows.forEachIndexed { i, (label, value) -> StatsRow(label, value, rowColor(i)) }
  }
}

@Composable
private fun StatsRow(label: String, value: String, background: Color) {
  Row(Modifier.fillMaxWidth().background(background).padding(horizontal = 14.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
    Text(label, Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
    Text(value, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
  }
}

@Composable
private fun rowColor(i: Int): Color =
  if (i % 2 == 0) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surfaceContainerHigh
