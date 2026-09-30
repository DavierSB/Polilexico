package app.lexico.ui.common

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

@Composable
fun ConfirmDialog(question: String, yes: String, no: String, onYes: () -> Unit, onNo: () -> Unit) {
  AlertDialog(
    onDismissRequest = onNo,
    text = { Text(question) },
    confirmButton = { TextButton(onClick = onYes) { Text(yes) } },
    dismissButton = { TextButton(onClick = onNo) { Text(no) } },
  )
}

@Composable
fun ResignDialog(onResign: () -> Unit, onContinue: () -> Unit) = ConfirmDialog(
  "¿Abandonar la partida? Se borra de partidas en curso y no cuenta en tus estadísticas.",
  yes = "Abandonar", no = "Seguir", onYes = onResign, onNo = onContinue,
)
