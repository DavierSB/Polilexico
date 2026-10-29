package app.lexico.ui.board

import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpOffset
import app.lexico.ui.common.LexicoIcons
import java.util.Locale

internal data class WordChoices(val at: Offset, val words: List<String>)

@Composable
internal fun WordMenu(choices: WordChoices?, onLookup: (String) -> Unit, onClose: () -> Unit) {
  val offset = with(LocalDensity.current) { choices?.let { DpOffset(it.at.x.toDp(), it.at.y.toDp()) } ?: DpOffset.Zero }
  DropdownMenu(expanded = choices != null, onDismissRequest = onClose, offset = offset) {
    choices?.words?.forEach { word ->
      DropdownMenuItem(
        text = { Text("Ver ${word.uppercase(Locale.ROOT)} en el diccionario") },
        leadingIcon = { Icon(LexicoIcons.Lookup, null, tint = MaterialTheme.colorScheme.primary) },
        onClick = { onClose(); onLookup(word) },
      )
    }
  }
}
