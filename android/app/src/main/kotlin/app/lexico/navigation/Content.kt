package app.lexico.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import app.lexico.game.Lexico
import app.lexico.home.HomeScreen
import app.lexico.home.MinigamesScreen
import app.lexico.menu.Settings
import app.lexico.menu.ThemeState
import app.lexico.modes.analysis.engineAnalyst
import app.lexico.modes.duplicate.toSetup
import app.lexico.modes.recall.toSetup
import app.lexico.modes.sprint.toSetup
import app.lexico.ui.analysis.AnalyzerScreen
import app.lexico.ui.classic.NewClassicScreen
import app.lexico.ui.classic.NewEndgameScreen
import app.lexico.ui.duplicate.NewDuplicateScreen
import app.lexico.ui.recall.NewRecallScreen
import app.lexico.ui.sprint.NewSprintScreen

@Composable
internal fun Content(screen: Screen, lexico: Lexico, nav: Navigator, settings: Settings, themes: ThemeState, onMenu: () -> Unit) {
  if (screen.isGame) GameScreens(screen, lexico, nav, themes) else OtherScreens(screen, lexico, nav, settings, onMenu)
}

@Composable
private fun GameScreens(screen: Screen, lexico: Lexico, nav: Navigator, themes: ThemeState) {
  when (screen) {
    is Screen.Classic -> ClassicRoute(screen, lexico, nav, themes)
    is Screen.Endgame -> EndgameRoute(screen, lexico, nav, themes)
    is Screen.Duplicate -> DuplicateRoute(screen, lexico, nav, themes)
    is Screen.Recall -> RecallRoute(screen, lexico, nav, themes)
    is Screen.Sprint -> SprintRoute(screen, lexico, nav, themes)
    is Screen.Continue -> ContinueRoute(screen, lexico, nav, themes)
    else -> Unit
  }
}

@Composable
private fun OtherScreens(screen: Screen, lexico: Lexico, nav: Navigator, settings: Settings, onMenu: () -> Unit) {
  val style = settings.theme.board(settings.lightBoard)
  when (screen) {
    Screen.Home -> HomeScreen(style, remember { lexico.savedGames.list().size }, lexico.demoGames::nextPlacements, onMenu, nav::go)
    Screen.Minigames -> MinigamesScreen(nav::back, nav::go)
    Screen.NewClassic -> NewClassicScreen(nav::back) { nav.replace(Screen.Classic(it)) }
    Screen.NewEndgame -> NewEndgameScreen(nav::back) { nav.replace(Screen.Endgame(it)) }
    Screen.NewDuplicate -> NewDuplicateScreen(nav::back, { lexico.duplicateRecords.best(it.toSetup()) }) { nav.replace(Screen.Duplicate(it)) }
    Screen.NewRecall -> NewRecallScreen(nav::back, { lexico.recallRecords.best(it.toSetup()) }) { nav.replace(Screen.Recall(it)) }
    Screen.NewSprint -> NewSprintScreen(nav::back, { lexico.sprintRecords.best(it.toSetup()) }) { nav.replace(Screen.Sprint(it)) }
    Screen.Analyzer -> AnalyzerScreen(style, remember(lexico) { engineAnalyst(lexico) }, nav::back)
    Screen.InProgress -> InProgressRoute(lexico, nav)
    Screen.Finished -> FinishedRoute(lexico, nav)
    is Screen.FinishedFolder -> FinishedFolderRoute(screen, lexico, nav)
    is Screen.Review -> ReviewRoute(screen, lexico, nav, style)
    Screen.Stats -> StatsRoute(lexico, settings, nav)
    else -> Unit
  }
}
