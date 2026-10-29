package app.lexico.ui.anagram

import androidx.compose.runtime.Composable
import app.lexico.ui.common.RulesDialog
import app.lexico.ui.common.RulesSection

@Composable
fun AnagramRulesDialog(close: () -> Unit) = RulesDialog("Reglas de Anagrama", ANAGRAM_RULES, close)

private val ANAGRAM_RULES = listOf(
  RulesSection(
    "En cada mano te dan unas letras que forman al menos una palabra válida: anágramalas contrarreloj, tocando las fichas " +
      "para ponerlas en orden.",
  ),
  RulesSection(
    "Las palabras tienen de ${Puzzles.MIN_TILES} a ${Puzzles.MAX_TILES} letras, y algunas letras pueden estar fijas en su sitio.",
  ),
  RulesSection(
    "Según la dificultad:",
    items = listOf(
      "Fácil: siempre hay varios anagramas y basta encontrar uno.",
      "Medio: puede haber uno solo o hasta ${Difficulty.MAX_ANSWERS}, y basta encontrar uno.",
      "Difícil: puede haber uno solo o hasta ${Difficulty.MAX_ANSWERS}, y hay que encontrarlos todos.",
    ),
  ),
  RulesSection(
    "Tienes un número contado de vidas (por defecto 3): enviar una palabra no válida o rendirte cuesta una. El reloj de la " +
      "serie solo corre mientras piensas una mano. La serie termina sin vidas o sin tiempo.",
  ),
  RulesSection("Puedes ajustar la dificultad, el tiempo y el número de vidas."),
)
