package app.lexico.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import app.lexico.game.Lexico
import app.lexico.game.race.RaceGame
import app.lexico.game.storage.Mode
import app.lexico.home.HomeScreen
import app.lexico.home.MenuCounts
import app.lexico.menu.Settings
import app.lexico.menu.ThemeState
import app.lexico.modes.analysis.engineAnagrams
import app.lexico.modes.analysis.engineAnalyst
import app.lexico.modes.anagram.toRace
import app.lexico.modes.anagram.toSetup
import app.lexico.modes.duplicate.toSetup
import app.lexico.modes.recall.toRace
import app.lexico.modes.recall.toSetup
import app.lexico.modes.sprint.toRace
import app.lexico.modes.sprint.toSetup
import app.lexico.ui.anagram.NewAnagramScreen
import app.lexico.ui.analysis.AnagrammerScreen
import app.lexico.ui.analysis.AnalyzerScreen
import app.lexico.ui.classic.NewClassicScreen
import app.lexico.ui.classic.NewEndgameScreen
import app.lexico.ui.duplicate.NewDuplicateScreen
import app.lexico.ui.recall.NewRecallScreen
import app.lexico.ui.sprint.NewSprintScreen

@Composable
internal fun Content(
  screen: Screen, lexico: Lexico, nav: Navigator, settings: Settings, themes: ThemeState, hasUpdate: Boolean, onMenu: () -> Unit,
) {
  if (screen.isGame) GameScreens(screen, lexico, nav, themes) else OtherScreens(screen, lexico, nav, settings, themes, hasUpdate, onMenu)
}

@Composable
private fun GameScreens(screen: Screen, lexico: Lexico, nav: Navigator, themes: ThemeState) {
  when (screen) {
    is Screen.Classic -> ClassicRoute(screen, lexico, nav, themes)
    is Screen.WifiHost -> WifiHostRoute(screen, lexico, nav, themes)
    is Screen.WifiJoin -> WifiJoinRoute(screen, lexico, nav, themes)
    is Screen.DuplicateWifiHost -> DuplicateHostRoute(screen, nav, themes)
    is Screen.DuplicateWifiJoin -> DuplicateJoinRoute(screen, nav, themes)
    is Screen.Endgame -> EndgameRoute(screen, lexico, nav, themes)
    is Screen.EndgameWifiHost -> EndgameHostRoute(screen, lexico, nav, themes)
    is Screen.EndgameWifiJoin -> EndgameJoinRoute(screen, lexico, nav, themes)
    is Screen.RaceHost -> RaceHostRoute(screen, lexico, nav, themes)
    is Screen.RaceJoin -> RaceJoinRoute(screen, lexico, nav, themes)
    is Screen.Duplicate -> DuplicateRoute(screen, lexico, nav, themes)
    is Screen.Recall -> RecallRoute(screen, lexico, nav, themes)
    is Screen.Sprint -> SprintRoute(screen, lexico, nav, themes)
    is Screen.Anagram -> AnagramRoute(screen, lexico, nav, themes)
    is Screen.Continue -> ContinueRoute(screen, lexico, nav, themes)
    else -> Unit
  }
}

@Composable
private fun OtherScreens(
  screen: Screen, lexico: Lexico, nav: Navigator, settings: Settings, themes: ThemeState, hasUpdate: Boolean, onMenu: () -> Unit,
) {
  val style = settings.theme.board(settings.boardLook)
  when (screen) {
    Screen.Home, Screen.ClassicMenu, Screen.DuplicateMenu, Screen.Minigames, Screen.Study, Screen.Lists ->
      HomeScreen(screen, style, rememberCounts(screen, lexico), lexico.demoGames::nextPlacements, hasUpdate, onMenu, nav::back, nav::go)
    Screen.NewClassic -> NewClassicScreen(
      nav::back, onStart = { nav.replace(Screen.Classic(it)) }, onHost = { config, name -> nav.replace(Screen.WifiHost(config, name)) },
    ) { nav.replace(Screen.WifiJoin(it)) }
    Screen.NewEndgame -> NewEndgameScreen(
      nav::back, onStart = { nav.replace(Screen.Endgame(it)) }, onHost = { config, name -> nav.replace(Screen.EndgameWifiHost(config, name)) },
    ) { nav.replace(Screen.EndgameWifiJoin(it)) }
    Screen.NewDuplicate -> NewDuplicateScreen(
      nav::back, { lexico.duplicateRecords.best(it.toSetup()) }, onStart = { nav.replace(Screen.Duplicate(it)) },
      onHost = { nav.replace(Screen.DuplicateWifiHost(it)) },
    ) { nav.replace(Screen.DuplicateWifiJoin(it)) }
    Screen.NewRecall -> NewRecallScreen(
      nav::back, { lexico.recallRecords.best(it.toSetup()) }, onStart = { nav.replace(Screen.Recall(it)) },
      onHost = { nav.replace(Screen.RaceHost(it.toRace(), it.name)) },
    ) { nav.replace(Screen.RaceJoin(RaceGame.RECALL, it)) }
    Screen.NewAnagram -> NewAnagramScreen(
      nav::back, { lexico.anagramRecords.best(it.toSetup()) }, onStart = { nav.replace(Screen.Anagram(it)) },
      onHost = { nav.replace(Screen.RaceHost(it.toRace(), it.name)) },
    ) { nav.replace(Screen.RaceJoin(RaceGame.ANAGRAM, it)) }
    Screen.NewSprint -> NewSprintScreen(
      nav::back, { lexico.sprintRecords.best(it.toSetup()) }, onStart = { nav.replace(Screen.Sprint(it)) },
      onHost = { nav.replace(Screen.RaceHost(it.toRace(), it.name)) },
    ) { nav.replace(Screen.RaceJoin(RaceGame.SPRINT, it)) }
    is Screen.Analyzer -> AnalyzerScreen(style, remember(lexico) { engineAnalyst(lexico) }, nav::back, screen.editor) { nav.go(Screen.PositionReview(it)) }
    is Screen.PositionReview -> PositionReviewRoute(screen, lexico, nav, themes)
    Screen.Anagrammer -> AnagrammerScreen(style, remember(lexico) { engineAnagrams(lexico) }, nav::back)
    Screen.NewList -> NewListRoute(lexico, nav)
    Screen.WordLists -> WordListsRoute(lexico, nav)
    Screen.UnfinishedStudies -> UnfinishedStudiesRoute(lexico, nav)
    Screen.CompletedStudies -> CompletedStudiesRoute(lexico, nav)
    is Screen.ListFolder -> ListFolderRoute(screen, lexico, nav)
    is Screen.WordListDetail -> WordListDetailRoute(screen, lexico, nav)
    is Screen.ListStudy -> ListStudyRoute(screen, lexico, nav)
    is Screen.ListTest -> ListTestRoute(screen, lexico, nav, themes)
    Screen.WordSearch -> WordSearchRoute(lexico, nav)
    is Screen.InProgress -> InProgressRoute(screen, lexico, nav)
    is Screen.FinishedFolder -> FinishedFolderRoute(screen, lexico, nav)
    is Screen.Review -> ReviewRoute(screen, lexico, nav, themes)
    Screen.Stats -> StatsRoute(lexico, settings, nav)
    else -> Unit
  }
}

@Composable
private fun rememberCounts(menu: Screen, lexico: Lexico): MenuCounts =
  remember(menu) { MenuCounts(savedOf(menu, lexico), if (menu == Screen.Lists) unfinishedStudies(lexico) else 0) }

private fun savedOf(menu: Screen, lexico: Lexico): Int {
  val mode = modeOf(menu) ?: return 0
  return lexico.savedGames.list().count { it.mode == mode }
}

private fun modeOf(menu: Screen): Mode? = when (menu) {
  Screen.ClassicMenu -> Mode.CLASSIC
  Screen.DuplicateMenu -> Mode.DUPLICATE
  else -> null
}
