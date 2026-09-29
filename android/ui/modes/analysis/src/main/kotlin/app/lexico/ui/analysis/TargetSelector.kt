package app.lexico.ui.analysis

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import app.lexico.ui.common.Hint
import app.lexico.ui.common.SectionTitle
import app.lexico.ui.common.TwoOptions

/** "Poner en: Tablero | Atril", con la ayuda de como se usa cada uno. */
@Composable
fun TargetSelector(target: Target, select: (Target) -> Unit) {
  Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
      SectionTitle("Poner en:")
      TwoOptions("Tablero", "Atril", target == Target.RACK) { select(if (it) Target.RACK else Target.BOARD) }
    }
    Hint(if (target == Target.RACK) RACK_HINT else BOARD_HINT)
  }
}

private val RACK_HINT = "Toca letras para añadirlas a tu atril (máx. ${PositionEditor.MAX_RACK})."
private const val BOARD_HINT = "Toca una casilla para poner la flecha (otra vez: vertical) y luego letras; o elige una letra " +
  "y toca casillas. Toca una ficha del tablero para quitarla."
