package app.lexico.home

import app.lexico.navigation.Screen

data class MenuCounts(val studies: Int)

internal data class MenuEntry(val name: String, val main: Boolean = true, val enabled: Boolean = true, val screen: () -> Screen)

internal fun menuTitle(menu: Screen): String = when (menu) {
  Screen.Minigames -> "Minijuegos"
  Screen.Study -> "Estudiar"
  Screen.Lists -> "Listas"
  else -> ""
}

internal fun menuInfo(menu: Screen): Pair<String, String>? = if (menu == Screen.Lists) "Listas" to LISTS_INFO else null

internal fun menuButtons(menu: Screen, counts: MenuCounts): List<MenuEntry> = when (menu) {
  Screen.Minigames -> MINIGAMES
  Screen.Study -> STUDY
  Screen.Lists -> lists(counts.studies)
  else -> HOME
}

private val HOME = listOf(
  MenuEntry("Clásica") { Screen.NewClassic },
  MenuEntry("Duplicada") { Screen.NewDuplicate },
  MenuEntry("Minijuegos") { Screen.Minigames },
  MenuEntry("Estudiar") { Screen.Study },
)

private val MINIGAMES = listOf(
  MenuEntry("Scrabble Sprint") { Screen.NewSprint },
  MenuEntry("Finales") { Screen.NewEndgame },
  MenuEntry("¿Cuántas recuerdas?") { Screen.NewRecall },
  MenuEntry("Anagrama") { Screen.NewAnagram },
)

private val STUDY = listOf(
  MenuEntry("Listas") { Screen.Lists },
  MenuEntry("Diccionario") { Screen.WordSearch },
  MenuEntry("Anagramador") { Screen.Anagrammer },
  MenuEntry("Analizador") { Screen.Analyzer() },
)

private fun lists(unfinished: Int): List<MenuEntry> = listOf(
  MenuEntry("Estudiar listas") { Screen.WordLists },
  MenuEntry("Crear lista") { Screen.NewList },
  MenuEntry("Estudios a medias ($unfinished)", main = false, enabled = unfinished > 0) { Screen.UnfinishedStudies },
  MenuEntry("Estudios culminados", main = false) { Screen.CompletedStudies },
)

private const val LISTS_INFO = "Aquí aparecen listas ya confeccionadas, con las palabras ordenadas por su importancia para el Scrabble. " +
  "Se dejaron fuera las palabras de uso más común en español, para que no estorben a la hora de estudiar.\n\n" +
  "Además, en «Crear lista» se pueden armar listas propias con los filtros que se quieran."
