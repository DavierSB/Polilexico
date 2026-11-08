package app.lexico.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class SavedShortcuts(val inProgress: Int, val onContinue: () -> Unit, val onFinished: () -> Unit)

@Composable
fun SavedButtons(saved: SavedShortcuts) {
  Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    OutlinedButton(onClick = saved.onContinue, Modifier.weight(1f), enabled = saved.inProgress > 0, contentPadding = RowButtonPadding) {
      ButtonText("En curso (${saved.inProgress})")
    }
    OutlinedButton(onClick = saved.onFinished, Modifier.weight(1f), contentPadding = RowButtonPadding) { ButtonText("Terminadas") }
  }
}
