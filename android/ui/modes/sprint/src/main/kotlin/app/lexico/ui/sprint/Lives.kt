package app.lexico.ui.sprint

import androidx.compose.foundation.layout.Arrangement
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

/** Abajo de la pantalla: las vidas como corazones (llenos los que quedan) y las manos resueltas. */
@Composable
fun Lives(lives: Int, maxLives: Int, solved: Int) {
  Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
    Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
      repeat(maxLives) { Heart(full = it < lives) }
    }
    Text("Resueltas: $solved", fontWeight = FontWeight.Bold)
  }
}

@Composable
private fun Heart(full: Boolean) {
  Text(
    if (full) "♥" else "♡", fontSize = 28.sp,
    color = if (full) HeartRed else MaterialTheme.colorScheme.outline,
  )
}
