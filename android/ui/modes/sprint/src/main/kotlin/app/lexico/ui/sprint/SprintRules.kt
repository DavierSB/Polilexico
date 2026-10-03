package app.lexico.ui.sprint

import androidx.compose.runtime.Composable
import app.lexico.ui.common.RulesDialog
import app.lexico.ui.common.RulesSection

@Composable
fun SprintRulesDialog(close: () -> Unit) = RulesDialog("Reglas de Scrabble Sprint", SPRINT_RULES, close)

private val SPRINT_RULES = listOf(
  RulesSection("Encuentra tantos scrabbles como puedas, dentro de un límite de tiempo y de intentos fallidos (vidas)."),
  RulesSection(
    "En cada ronda se muestra un tablero distinto, con un atril que tiene al menos un scrabble: encuéntralo, no importa " +
      "si no es el de mayor puntuación.",
  ),
  RulesSection(
    "Si no lo encuentras, puedes rendirte y pasar a otro tablero, pero pierdes una vida. En modo Single, realizar una " +
      "jugada no válida también cuesta una vida.",
  ),
  RulesSection("Puedes ajustar la dificultad, el tiempo, el número de vidas y la comprobación de jugadas."),
)
