package app.lexico.ui.games

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun OpponentSelector(selected: String?, opponents: List<String>, photo: @Composable (String) -> Unit, select: (String?) -> Unit) {
  var open by remember { mutableStateOf(false) }
  Box {
    OutlinedButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth()) { OpponentLabel(selected, photo, "  ▾") }
    DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
      (listOf(null) + opponents).forEach { opponent ->
        DropdownMenuItem(text = { OpponentLabel(opponent, photo) }, onClick = { select(opponent); open = false })
      }
    }
  }
}

@Composable
private fun OpponentLabel(opponent: String?, photo: @Composable (String) -> Unit, suffix: String = "") {
  Row(verticalAlignment = Alignment.CenterVertically) {
    if (opponent != null) {
      photo(opponent)
      Spacer(Modifier.width(8.dp))
    }
    Text((opponent ?: "Todos los rivales") + suffix)
  }
}
