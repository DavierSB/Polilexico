package app.lexico.ui.recall

import androidx.compose.runtime.Composable
import app.lexico.ui.common.RulesDialog
import app.lexico.ui.common.RulesSection

@Composable
fun RecallRulesDialog(close: () -> Unit) = RulesDialog("Reglas de ¿Cuántas recuerdas?", RECALL_RULES, close)

private val RECALL_RULES = listOf(
  RulesSection(
    "En cada ronda, se reproducirá ante tus ojos una partida que durará unos pocos segundos. Luego, debes anagramar " +
      "las palabras más valiosas que se hayan jugado.",
  ),
  RulesSection(
    "Tienes un número contado de vidas (por defecto 3): cada palabra que no recuerdes cuesta una. En modo Single, " +
      "realizar una jugada no válida también cuesta una vida.",
  ),
  RulesSection(
    "Puedes ajustar el número de vidas y la velocidad a la que se juega la partida de muestra.",
  ),
)
