package app.lexico.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val HeartRed = Color(0xFFE5484D)

@Composable
fun Lives(lives: Int, maxLives: Int, label: String, solved: Int, best: Int) {
  Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
    Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
      repeat(maxLives) { Heart(full = it < lives) }
    }
    Column(horizontalAlignment = Alignment.End) {
      Text("$label: $solved", fontWeight = FontWeight.Bold)
      RecordToBeat(solved, best)
    }
  }
}

@Composable
private fun RecordToBeat(solved: Int, best: Int) {
  if (best == 0) return
  if (solved > best) Text("¡Nuevo récord!", color = MaterialTheme.colorScheme.primary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
  else Text("Récord: $best", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
}

@Composable
private fun Heart(full: Boolean) {
  Text(
    if (full) "♥" else "♡", fontSize = 28.sp,
    color = if (full) HeartRed else MaterialTheme.colorScheme.outline,
  )
}
