package app.lexico.ui.common

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun InfoButton(title: String, text: String, modifier: Modifier = Modifier) {
  var open by remember { mutableStateOf(false) }
  Box(modifier.clickable { open = true }.padding(6.dp), Alignment.Center) { InfoIcon() }
  if (open) InfoDialog(title, text) { open = false }
}

@Composable
fun InfoIcon(modifier: Modifier = Modifier) {
  val color = MaterialTheme.colorScheme.primary
  Box(modifier.size(16.dp).border(1.5.dp, color, CircleShape), Alignment.Center) {
    Text("i", style = TextStyle(fontFamily = Mulish, fontWeight = FontWeight.Black, fontSize = 11.sp, lineHeight = 11.sp, color = color))
  }
}

@Composable
fun InfoDialog(title: String, text: String, close: () -> Unit) {
  AlertDialog(
    onDismissRequest = close,
    title = { Text(title) },
    text = { Text(text) },
    confirmButton = { TextButton(onClick = close) { Text("Entendido") } },
  )
}
