package app.lexico.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun NoProfileCard(online: Int?, onCreate: () -> Unit) {
  Column(
    Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp)).padding(18.dp),
    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    Text("👥", fontSize = 34.sp)
    Text("Jugar en línea", fontWeight = FontWeight.Bold)
    online?.takeIf { it > 0 }?.let { Text(connectedLabel(it), color = MaterialTheme.colorScheme.onSurfaceVariant) }
    Button(onClick = onCreate, Modifier.fillMaxWidth()) { ButtonText("Crear perfil") }
  }
}

private fun connectedLabel(count: Int): String = if (count == 1) "1 persona conectada" else "$count personas conectadas"
