package app.lexico.ui.duplicate

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.lexico.ui.board.BoardStyle
import app.lexico.ui.board.RackTile

@Composable
fun InvalidRack(rack: List<String>, style: BoardStyle) {
  Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
    InvalidRackLabel()
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally)) {
      rack.forEach { RackTile(it, style, size = 42.dp) }
    }
  }
}

@Composable
private fun InvalidRackLabel() {
  Text(
    "¡Mano inválida!",
    Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.errorContainer, RoundedCornerShape(10.dp)).padding(vertical = 8.dp),
    color = MaterialTheme.colorScheme.onErrorContainer, fontSize = 22.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
  )
}
