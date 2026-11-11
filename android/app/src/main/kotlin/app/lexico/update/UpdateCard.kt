package app.lexico.update

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val UPDATE_YELLOW = Color(0xFFFFC107)

@Composable
internal fun UpdateCard(version: String, progress: Float?, onUpdate: () -> Unit, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
  ElevatedCard(modifier.widthIn(max = 480.dp).fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
      UpdateHeader(version)
      UpdateButtons(progress, onUpdate, onDismiss)
    }
  }
}

@Composable
private fun UpdateHeader(version: String) {
  Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
    UpdateBadge()
    Column {
      Text("Poliléxico $version", fontWeight = FontWeight.Bold)
      Text("Nueva versión", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
  }
}

@Composable
private fun UpdateBadge() {
  Box(Modifier.size(44.dp).background(UPDATE_YELLOW, CircleShape), contentAlignment = Alignment.Center) {
    Text("↑", color = Color.Black, fontSize = 24.sp, fontWeight = FontWeight.Bold)
  }
}

@Composable
private fun UpdateButtons(progress: Float?, onUpdate: () -> Unit, onDismiss: () -> Unit) {
  Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    OutlinedButton(onClick = onDismiss, Modifier.weight(1f)) { Text("Ahora no") }
    Button(onClick = { if (progress == null) onUpdate() }, Modifier.weight(1f), colors = yellowButton()) { Text(updateLabel(progress)) }
  }
}

@Composable
private fun yellowButton() = ButtonDefaults.buttonColors(containerColor = UPDATE_YELLOW, contentColor = Color.Black)

private fun updateLabel(progress: Float?): String = progress?.let { "${(it * 100).toInt()}%" } ?: "Actualizar"
