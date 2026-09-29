package app.lexico.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Barra superior de una partida: salir (la partida queda guardada), abandonar (solo si sigue en
 * juego), pausar (en las partidas con tiempo), cambiar el tema, la tabla de movidas y la bolsita
 * con las fichas que quedan.
 */
@Composable
fun GameBar(
  bag: Int,
  onExit: () -> Unit,
  onMoves: () -> Unit,
  onBag: () -> Unit,
  onResign: (() -> Unit)?,
  onPause: (() -> Unit)? = null,
  onTheme: (() -> Unit)? = null,
) {
  Row(Modifier.fillMaxWidth().height(44.dp), Arrangement.spacedBy(2.dp), Alignment.CenterVertically) {
    ExitButton(onExit)
    if (onResign != null) BarTextButton("Abandonar", onResign)
    Spacer(Modifier.weight(1f))
    if (onPause != null) PauseButton(onPause)
    if (onTheme != null) ThemeButton(onTheme)
    BarTextButton("Movidas", onMoves)
    BagIcon(bag, onBag)
  }
}

/** Cabecera de las pantallas que no son partidas: volver, el titulo y, a la derecha, `extra`. */
@Composable
fun Header(title: String, onBack: () -> Unit, extra: @Composable () -> Unit = {}) {
  Row(Modifier.fillMaxWidth().height(48.dp), Arrangement.spacedBy(6.dp), Alignment.CenterVertically) {
    BarIconButton(LexicoIcons.Back, "Volver", onBack)
    BarTitle(title, Modifier.weight(1f).padding(start = 6.dp))
    extra()
  }
}

/** El titulo de una barra, en Mulish; si no cabe, se achica antes de cortarse. */
@Composable
fun BarTitle(title: String, modifier: Modifier = Modifier) {
  BasicText(
    title, modifier, maxLines = 1, overflow = TextOverflow.Ellipsis,
    autoSize = TextAutoSize.StepBased(minFontSize = 15.sp, maxFontSize = 21.sp),
    style = TextStyle(fontFamily = Mulish, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface),
  )
}

/** "Salir": la partida queda guardada. */
@Composable
fun ExitButton(onClick: () -> Unit) = BarTextButton("Salir", onClick)

@Composable
fun ThemeButton(onClick: () -> Unit) = BarIconButton(LexicoIcons.Palette, "Tema", onClick)

/** Un boton de barra con su icono, sin fondo; `description` es lo que lee el lector de pantalla. */
@Composable
fun BarIconButton(icon: ImageVector, description: String, onClick: () -> Unit) {
  IconButton(onClick, Modifier.size(40.dp), colors = barIconColors()) { Icon(icon, description, Modifier.size(22.dp)) }
}

/** Una accion de barra con texto ("Abandonar", "Movidas", "Vaciar"...), sin fondo. */
@Composable
fun BarTextButton(text: String, onClick: () -> Unit) {
  TextButton(onClick, Modifier.height(36.dp), contentPadding = PaddingValues(horizontal = 8.dp)) { Text(text, style = barLabel()) }
}

@Composable
internal fun barIconColors(): IconButtonColors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.primary)

/** Un dato de la barra en una pastilla: "RONDA 1/3". */
@Composable
fun BarChip(text: String) {
  Text(
    text.uppercase(),
    Modifier.background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(50)).padding(horizontal = 10.dp, vertical = 5.dp),
    style = TextStyle(fontFamily = Mulish, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, letterSpacing = 1.sp,
      color = MaterialTheme.colorScheme.onPrimaryContainer),
  )
}

private fun barLabel(): TextStyle = TextStyle(fontFamily = Mulish, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
