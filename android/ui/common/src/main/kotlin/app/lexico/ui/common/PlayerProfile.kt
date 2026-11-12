package app.lexico.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class ProfileOpener(val open: (name: String, country: String) -> Unit)

val LocalProfileOpener = compositionLocalOf<ProfileOpener?> { null }

class ProfileButtons(
  val write: (() -> Unit)? = null, val challenge: (() -> Unit)? = null, val watch: (() -> Unit)? = null, val resume: (() -> Unit)? = null,
)

@Composable
fun Modifier.opensProfile(name: String, country: String): Modifier {
  val opener = LocalProfileOpener.current ?: return this
  return clickable { opener.open(name, country) }
}

@Composable
fun PlayerProfileDialog(name: String, country: String, sections: List<StatsSection>?, error: String?, buttons: ProfileButtons, close: () -> Unit) {
  AlertDialog(
    onDismissRequest = close,
    icon = { PersonAvatar(name, country, size = 72.dp) },
    title = { Text("$name  ${flagOf(country)}") },
    text = { ProfileStats(sections, error) },
    confirmButton = { MainAction(buttons, close) },
    dismissButton = buttons.write?.let { write -> { OutlinedButton(onClick = { close(); write() }) { Text("$CHAT_GLYPH Escribir") } } },
  )
}

@Composable
private fun ProfileStats(sections: List<StatsSection>?, error: String?) {
  Column(Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
    when {
      error != null -> Text(error, color = MaterialTheme.colorScheme.error)
      sections == null -> CircularProgressIndicator()
      sections.isEmpty() -> Text("Aún no tiene partidas en línea.")
      else -> sections.forEach { StatsCard(it) }
    }
  }
}

@Composable
private fun MainAction(buttons: ProfileButtons, close: () -> Unit) {
  val main = buttons.watch?.let { "Ver" to it } ?: buttons.resume?.let { "Retomar" to it } ?: buttons.challenge?.let { "Retar" to it }
  if (main == null) return TextButton(onClick = close) { Text("Cerrar") }
  Button(onClick = { close(); main.second() }) { Text(main.first) }
}
