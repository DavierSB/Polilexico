package app.lexico.ui.board

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.lexico.model.Letters

@Composable
fun BlankLetterDialog(onPick: (String?) -> Unit) {
  AlertDialog(
    onDismissRequest = { onPick(null) },
    title = { Text("¿Qué letra es el comodín?") },
    text = { LetterGrid(onPick) },
    confirmButton = { TextButton(onClick = { onPick(null) }) { Text("Cancelar") } },
  )
}

@Composable
private fun LetterGrid(onPick: (String) -> Unit) {
  FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
    Letters.ALL.forEach { LetterCell(it, onPick) }
  }
}

@Composable
private fun LetterCell(letter: String, onPick: (String) -> Unit) {
  Box(
    Modifier.size(40.dp).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(6.dp)).clickable { onPick(letter) },
    contentAlignment = Alignment.Center,
  ) { Text(letter, fontWeight = FontWeight.Bold, fontSize = 15.sp) }
}
