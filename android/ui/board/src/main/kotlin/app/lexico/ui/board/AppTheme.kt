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
data class AppTheme(val name: String, val colors: ColorScheme, val dark: BoardStyle, val light: BoardStyle) {
  fun board(lightBoard: Boolean): BoardStyle = if (lightBoard) light else dark
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
  current: AppTheme, lightBoard: Boolean, choose: (AppTheme) -> Unit, chooseLight: (Boolean) -> Unit, close: () -> Unit,
) {
  AlertDialog(
    onDismissRequest = close,
    confirmButton = { TextButton(onClick = close) { Text("Cerrar") } },
    title = { Text("Tema") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AppThemes.ALL.forEach { ThemeChoice(it, lightBoard, selected = it == current) { choose(it); close() } }
        LightBoardSwitch(lightBoard, chooseLight)
      }
    },
  )
}

@Composable
private fun LightBoardSwitch(lightBoard: Boolean, chooseLight: (Boolean) -> Unit) {
  Row(
    Modifier.fillMaxWidth().padding(top = 8.dp).toggleable(lightBoard, role = Role.Switch, onValueChange = chooseLight),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text("Tablero claro", Modifier.weight(1f))
    Switch(checked = lightBoard, onCheckedChange = null)
  }
}

@Composable
private fun ThemeChoice(theme: AppTheme, lightBoard: Boolean, selected: Boolean, choose: () -> Unit) {
  Row(
    Modifier.fillMaxWidth().selectable(selected, role = Role.RadioButton, onClick = choose),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    RadioButton(selected = selected, onClick = null)
    Text(theme.name, Modifier.padding(start = 8.dp).weight(1f))
    Swatches(theme, theme.board(lightBoard))
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
