package app.lexico.navigation

import app.lexico.game.race.RaceGame
import app.lexico.game.race.RaceSetup
import app.lexico.game.storage.Mode
import app.lexico.game.storage.SavedGame
import app.lexico.ui.anagram.AnagramConfig
import app.lexico.ui.analysis.EditedPosition
import app.lexico.ui.analysis.PositionEditor
import app.lexico.ui.classic.ClassicConfig
import app.lexico.ui.duplicate.WifiDuplicateConfig
import app.lexico.ui.classic.EndgameConfig
import app.lexico.ui.duplicate.DuplicateConfig
import app.lexico.ui.games.GameFolder
import app.lexico.ui.recall.RecallConfig
import app.lexico.ui.sprint.SprintConfig

sealed interface Screen {
  data object Home : Screen
  data object Study : Screen
  data object ClassicMenu : Screen
  data object DuplicateMenu : Screen
  data object NewClassic : Screen
  data class Classic(val config: ClassicConfig) : Screen
  data class WifiHost(val config: ClassicConfig, val name: String) : Screen
  data class WifiJoin(val name: String) : Screen
  data class DuplicateWifiHost(val config: WifiDuplicateConfig) : Screen
  data class DuplicateWifiJoin(val name: String) : Screen
  data object NewEndgame : Screen
  data class Endgame(val config: EndgameConfig) : Screen
  data class EndgameWifiHost(val config: EndgameConfig, val name: String) : Screen
  data class EndgameWifiJoin(val name: String) : Screen
  data class RaceHost(val setup: RaceSetup, val name: String) : Screen
  data class RaceJoin(val game: RaceGame, val name: String) : Screen
  data object NewDuplicate : Screen
  data class Duplicate(val config: DuplicateConfig) : Screen
  data object Minigames : Screen
  data object NewRecall : Screen
  data class Recall(val config: RecallConfig) : Screen
  data object NewSprint : Screen
  data class Sprint(val config: SprintConfig, val run: Int = 0) : Screen
  data object NewAnagram : Screen
  data class Anagram(val config: AnagramConfig) : Screen
  data class Analyzer(val editor: PositionEditor = PositionEditor()) : Screen
  data class PositionReview(val position: EditedPosition) : Screen
  data object Anagrammer : Screen
  data object Lists : Screen
  data object NewList : Screen
  data object WordLists : Screen
  data object UnfinishedStudies : Screen
  data object CompletedStudies : Screen
  data class ListFolder(val path: List<String>) : Screen
  data class WordListDetail(val id: String) : Screen
  data class ListStudy(val id: String) : Screen
  data class ListTest(val id: String) : Screen
  data object WordSearch : Screen
  data class InProgress(val mode: Mode) : Screen
  data class Continue(val saved: SavedGame) : Screen
  data class FinishedFolder(val folder: GameFolder) : Screen
  data class Review(val path: String, val atEndgame: Boolean = false) : Screen
  data object Stats : Screen

  val isGame: Boolean get() = this is Classic || this is WifiHost || this is WifiJoin || this is DuplicateWifiHost || this is DuplicateWifiJoin || this is Endgame ||
    this is EndgameWifiHost || this is EndgameWifiJoin || this is RaceHost || this is RaceJoin || this is Duplicate || this is Recall || this is Sprint || this is Anagram || this is Continue

  val hasBoard: Boolean get() = isGame || this is Analyzer || this is PositionReview || this is Review

  val isActivity: Boolean get() = hasBoard || this is Anagrammer || this is ListStudy || this is ListTest || this is Stats
}
