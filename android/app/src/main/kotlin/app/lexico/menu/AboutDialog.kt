package app.lexico.menu

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp

/** Donde esta el codigo fuente: la GPL pide ofrecerlo a quien recibe la app. */
private const val SOURCE_URL = "https://github.com/DavierSB/polilexico"

/** El dialogo "Acerca de" del menu lateral, como el de WooglesMovil; el autor, en otro dialogo. */
@Composable
fun AboutDialog(version: String, close: () -> Unit) {
  var author by remember { mutableStateOf(false) }
  AlertDialog(
    onDismissRequest = close,
    confirmButton = { TextButton(onClick = close) { Text("Cerrar") } },
    title = { Thanks() },
    text = { AboutText(version) { author = true } },
  )
  if (author) AuthorDialog { author = false }
}

/** "¡Gracias por usar Poliléxico!", con un corazon ambar al lado. */
@Composable
private fun Thanks() {
  Text(buildAnnotatedString {
    append("¡Gracias por usar Poliléxico! ")
    withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) { append("♥") }
  })
}

@Composable
private fun AboutText(version: String, showAuthor: () -> Unit) {
  Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    Text(
      "El propósito de Poliléxico es ser la primera aplicación offline para móvil que combine las diferentes " +
        "modalidades del Scrabble con todas sus reglas precisas, y además brinde herramientas didácticas para su aprendizaje."
    )
    Text(
      "El motor de juego es el de Woogles.io (Macondo), usado bajo los términos de su licencia, " +
        "la GNU General Public License v3."
    )
    Text(
      "Los avatares de los bots son un tributo a la cultura hispanoamericana, y en especial a la cubana."
    )
    Text("Hecha con amor desde Cuba.")
    Link("Acerca del autor", showAuthor)
    License(version)
  }
}

/** La version, el copyright, la licencia y el enlace al codigo fuente. */
@Composable
private fun License(version: String) {
  val uriHandler = LocalUriHandler.current
  Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
    Text(
      "Versión $version. © 2026 Davier Bello. Poliléxico es software libre: se distribuye bajo la GNU General " +
        "Public License, versión 3 o (a tu elección) cualquier versión posterior, con los términos adicionales " +
        "que acompañan al código.",
      style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Link("Código fuente") { uriHandler.openUri(SOURCE_URL) }
  }
}

@Composable
private fun Link(text: String, onClick: () -> Unit) {
  Text(
    text, Modifier.clickable(onClick = onClick), color = MaterialTheme.colorScheme.primary,
    style = MaterialTheme.typography.bodySmall.copy(textDecoration = TextDecoration.Underline),
  )
}

@Composable
private fun AuthorDialog(close: () -> Unit) {
  AlertDialog(
    onDismissRequest = close,
    confirmButton = { TextButton(onClick = close) { Text("Cerrar") } },
    text = {
      Column {
        Text("Davier Bello")
        Text("daviersanbell@gmail.com", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
    },
  )
}
