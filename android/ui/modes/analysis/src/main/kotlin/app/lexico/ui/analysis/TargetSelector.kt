package app.lexico.ui.analysis

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import app.lexico.ui.common.SectionTitle
import app.lexico.ui.common.TwoOptions

/** "Poner en: Tablero | Atril", con una ⓘ que explica como se usa cada uno. */
@Composable
fun TargetSelector(target: Target, select: (Target) -> Unit) {
  Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
    SectionTitle("Poner en:", info = "$BOARD_HINT\n\n$RACK_HINT")
    TwoOptions("Tablero", "Atril", target == Target.RACK) { select(if (it) Target.RACK else Target.BOARD) }
  }
}

private val RACK_HINT = "Atril: toca letras para añadirlas (máx. ${PositionEditor.MAX_RACK})."
private const val BOARD_HINT = "Tablero: toca una casilla para poner la flecha (otra vez: vertical) y luego letras; o elige una letra " +
  "y toca casillas. Toca una ficha del tablero para quitarla."
