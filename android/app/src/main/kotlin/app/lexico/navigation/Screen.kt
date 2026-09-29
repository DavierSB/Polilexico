package app.lexico.navigation

import app.lexico.game.storage.SavedGame
import app.lexico.ui.classic.ClassicConfig
import app.lexico.ui.classic.EndgameConfig
import app.lexico.ui.duplicate.DuplicateConfig
import app.lexico.ui.games.GameFolder
import app.lexico.ui.recall.RecallConfig
import app.lexico.ui.sprint.SprintConfig

/** Las pantallas de la aplicacion. */
sealed interface Screen {
  data object Home : Screen
  data object NewClassic : Screen
  data class Classic(val config: ClassicConfig) : Screen
  data object NewEndgame : Screen
  /** Finales: una clasica que empieza en un final buscado con estas opciones. */
  data class Endgame(val config: EndgameConfig) : Screen
  data object NewDuplicate : Screen
  data class Duplicate(val config: DuplicateConfig) : Screen
  data object Minigames : Screen
  data object NewRecall : Screen
  data class Recall(val config: RecallConfig) : Screen
  data object NewSprint : Screen
  /** Una serie de Scrabble Sprint; `run` distingue una serie de la siguiente con las mismas opciones. */
  data class Sprint(val config: SprintConfig, val run: Int = 0) : Screen
  data object Analyzer : Screen
  data object InProgress : Screen
  /** Continuar una partida guardada (clasica, Finales o duplicada). */
  data class Continue(val saved: SavedGame) : Screen
  /** "Mis partidas": las carpetas de las terminadas. */
  data object Finished : Screen
  /** Las partidas terminadas de una carpeta. */
  data class FinishedFolder(val folder: GameFolder) : Screen
  /** Revisar la partida terminada del registro `path`. */
  data class Review(val path: String) : Screen
  data object Stats : Screen

  /** En las partidas el tablero ocupa casi todo el ancho y el menu no se abre deslizando. */
  val isGame: Boolean get() = this is Classic || this is Endgame || this is Duplicate || this is Recall || this is Sprint || this is Continue

  /** Pantallas con tablero: casi sin margen lateral, para que se vea mas grande. */
  val hasBoard: Boolean get() = isGame || this == Analyzer || this is Review
}
