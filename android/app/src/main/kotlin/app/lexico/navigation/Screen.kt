package app.lexico.navigation

import app.lexico.game.storage.SavedGame
import app.lexico.ui.classic.ClassicConfig
import app.lexico.ui.classic.EndgameConfig
import app.lexico.ui.duplicate.DuplicateConfig
import app.lexico.ui.games.GameFolder
import app.lexico.ui.recall.RecallConfig
import app.lexico.ui.sprint.SprintConfig

sealed interface Screen {
  data object Home : Screen
  data object NewClassic : Screen
  data class Classic(val config: ClassicConfig) : Screen
  data object NewEndgame : Screen
  data class Endgame(val config: EndgameConfig) : Screen
  data object NewDuplicate : Screen
  data class Duplicate(val config: DuplicateConfig) : Screen
  data object Minigames : Screen
  data object NewRecall : Screen
  data class Recall(val config: RecallConfig) : Screen
  data object NewSprint : Screen
  data class Sprint(val config: SprintConfig, val run: Int = 0) : Screen
  data object Analyzer : Screen
  data object InProgress : Screen
  data class Continue(val saved: SavedGame) : Screen
  data object Finished : Screen
  data class FinishedFolder(val folder: GameFolder) : Screen
  data class Review(val path: String) : Screen
  data object Stats : Screen

  val isGame: Boolean get() = this is Classic || this is Endgame || this is Duplicate || this is Recall || this is Sprint || this is Continue

  val hasBoard: Boolean get() = isGame || this == Analyzer || this is Review
}
