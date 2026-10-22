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
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
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

@Immutable
data class BoardLook(val light: Boolean = false, val swapped: Boolean = false)

@Immutable
data class AppTheme(val name: String, val colors: ColorScheme, val dark: BoardStyle, val light: BoardStyle) {
  fun board(look: BoardLook): BoardStyle = shade(look.light).let { if (look.swapped) it.swapped() else it }

  private fun shade(lightBoard: Boolean): BoardStyle = if (lightBoard) light else dark
}

object AppThemes {
  val Polimita = AppTheme("Polimita", PolimitaColors, BoardStyles.Polimita, BoardStyles.PolimitaLight)
  val Leaf = AppTheme("Hoja", LeafColors, BoardStyles.Leaf, BoardStyles.LeafLight)
  val Sky = AppTheme("Celeste", SkyColors, BoardStyles.Sky, BoardStyles.SkyLight)
  val Night = AppTheme("Noche", NightColors, BoardStyles.Night, BoardStyles.NightLight)

  val ALL = listOf(Polimita, Leaf, Sky, Night)

  fun byName(name: String?): AppTheme = ALL.find { it.name == name } ?: Polimita
}

@Composable
fun Themed(theme: AppTheme, content: @Composable () -> Unit) {
  LexicoTheme(theme.colors, content)
}

@Composable
fun ThemeDialog(
  current: AppTheme, look: BoardLook, choose: (AppTheme) -> Unit, chooseLook: (BoardLook) -> Unit, close: () -> Unit,
) {
  AlertDialog(
    onDismissRequest = close,
    confirmButton = { TextButton(onClick = close) { Text("Cerrar") } },
    title = { Text("Tema") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AppThemes.ALL.forEach { ThemeChoice(it, look, selected = it == current) { choose(it); close() } }
        LookSwitch("Tablero claro", look.light) { chooseLook(look.copy(light = it)) }
        LookSwitch("Intercambiar dobles y triples", look.swapped) { chooseLook(look.copy(swapped = it)) }
      }
    },
  )
}

@Composable
private fun LookSwitch(label: String, on: Boolean, toggle: (Boolean) -> Unit) {
  Row(
    Modifier.fillMaxWidth().padding(top = 8.dp).toggleable(on, role = Role.Switch, onValueChange = toggle),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(label, Modifier.weight(1f))
    Switch(checked = on, onCheckedChange = null)
  }
}

@Composable
private fun ThemeChoice(theme: AppTheme, look: BoardLook, selected: Boolean, choose: () -> Unit) {
  Row(
    Modifier.fillMaxWidth().selectable(selected, role = Role.RadioButton, onClick = choose),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    RadioButton(selected = selected, onClick = null)
    Text(theme.name, Modifier.padding(start = 8.dp).weight(1f))
    Swatches(theme, theme.board(look))
  }
}

@Composable
private fun Swatches(theme: AppTheme, board: BoardStyle) {
  Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
    listOf(theme.colors.primary, board.tile, board.tripleWord).forEach { Dot(it) }
  }
}

@Composable
private fun Dot(color: Color) {
  Box(Modifier.size(16.dp).background(color, CircleShape))
}
