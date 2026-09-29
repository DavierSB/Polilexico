package app.lexico.ui.analysis

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.lexico.model.Letters
import app.lexico.ui.board.BoardStyle
import app.lexico.ui.board.RackTile

/** El teclado del analizador: todas las letras y el comodin, como fichas. `marked` = elegida. */
@Composable
fun LetterPalette(style: BoardStyle, marked: String?, modifier: Modifier = Modifier, onLetter: (String) -> Unit) {
  FlowRow(
    modifier.verticalScroll(rememberScrollState()),
    horizontalArrangement = Arrangement.spacedBy(4.dp),
    verticalArrangement = Arrangement.spacedBy(4.dp),
  ) {
    (Letters.ALL + Letters.BLANK).forEach { PaletteKey(it, style, marked = it == marked) { onLetter(it) } }
  }
}

@Composable
private fun PaletteKey(letter: String, style: BoardStyle, marked: Boolean, onClick: () -> Unit) {
  Box(Modifier.clickable(onClick = onClick), contentAlignment = Alignment.Center) {
    RackTile(letter, style, size = 36.dp, marked = marked)
    // La ficha del comodin va en blanco; en la paleta se marca con "?".
    if (letter == Letters.BLANK) Text("?", color = style.blank, fontWeight = FontWeight.Bold)
  }
}
