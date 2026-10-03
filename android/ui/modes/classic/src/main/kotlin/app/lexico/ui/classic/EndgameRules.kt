package app.lexico.ui.classic

import androidx.compose.runtime.Composable
import app.lexico.ui.common.RulesDialog
import app.lexico.ui.common.RulesSection

@Composable
fun EndgameRulesDialog(close: () -> Unit) = RulesDialog("Reglas de Finales", ENDGAME_RULES, close)

private val ENDGAME_RULES = listOf(
  RulesSection("Tomarás el control de una partida Clásica en sus compases finales, para que intentes ganarla."),
  RulesSection(
    "Puedes ajustar ciertas características del final de la partida, como el número de fichas que quedan en la bolsa " +
      "(por defecto, entre 2 y 8), el margen de diferencia entre tú y tu rival (por defecto, vas perdiendo por entre " +
      "0 y 40 puntos) o dónde está la Q (por defecto, puede o no haberse jugado).",
  ),
)
