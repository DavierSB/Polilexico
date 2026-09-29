package app.lexico.ui.board

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import app.lexico.ui.common.LeafColors
import app.lexico.ui.common.LexicoTheme
import app.lexico.ui.common.NightColors
import app.lexico.ui.common.PolimitaColors
import app.lexico.ui.common.SkyColors

/** Un tema de la aplicacion: la paleta (botones, relojes, bordes, textos) y el tablero con sus fichas. */
@Immutable
data class AppTheme(val name: String, val colors: ColorScheme, val board: BoardStyle)

/** Los temas. [Polimita] es el principal; los demas son los de algunos rivales. */
object AppThemes {
  val Polimita = AppTheme("Polimita", PolimitaColors, BoardStyles.Polimita)
  val Leaf = AppTheme("Hoja", LeafColors, BoardStyles.Leaf)
  val Sky = AppTheme("Celeste", SkyColors, BoardStyles.Sky)
  val Night = AppTheme("Noche", NightColors, BoardStyles.Night)

  val ALL = listOf(Polimita, Leaf, Sky, Night)

  /** El tema con ese nombre, o [Polimita] si no hay ninguno. */
  fun byName(name: String?): AppTheme = ALL.find { it.name == name } ?: Polimita
}

/** Lo de dentro, con la paleta del tema. */
@Composable
fun Themed(theme: AppTheme, content: @Composable () -> Unit) {
  LexicoTheme(theme.colors, content)
}

/** Elegir un tema: cada uno con su nombre y sus colores (el principal, las fichas y un premio). */
@Composable
fun ThemeDialog(current: AppTheme, choose: (AppTheme) -> Unit, close: () -> Unit) {
  AlertDialog(
    onDismissRequest = close,
    confirmButton = { TextButton(onClick = close) { Text("Cerrar") } },
    title = { Text("Tema") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AppThemes.ALL.forEach { ThemeChoice(it, selected = it == current) { choose(it); close() } }
      }
    },
  )
}

@Composable
private fun ThemeChoice(theme: AppTheme, selected: Boolean, choose: () -> Unit) {
  Row(
    Modifier.fillMaxWidth().selectable(selected, role = Role.RadioButton, onClick = choose),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    RadioButton(selected = selected, onClick = null)
    Text(theme.name, Modifier.padding(start = 8.dp).weight(1f))
    Swatches(theme)
  }
}

/** Tres puntos de color: el principal del tema, sus fichas y su premio mas fuerte. */
@Composable
private fun Swatches(theme: AppTheme) {
  Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
    listOf(theme.colors.primary, theme.board.tile, theme.board.tripleWord).forEach { Dot(it) }
  }
}

@Composable
private fun Dot(color: Color) {
  Box(Modifier.size(16.dp).background(color, CircleShape))
}
