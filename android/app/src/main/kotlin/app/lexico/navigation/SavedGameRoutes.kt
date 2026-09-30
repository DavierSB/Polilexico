package app.lexico.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import app.lexico.game.Lexico
import app.lexico.game.modes.classic.ClassicGame
import app.lexico.game.modes.duplicate.DuplicateGame
import app.lexico.games.finishedItem
import app.lexico.games.folderOf
import app.lexico.games.inProgressItem
import app.lexico.games.reviewView
import app.lexico.games.statsPage
import app.lexico.menu.Settings
import app.lexico.menu.ThemeState
import app.lexico.ui.board.BoardStyle
import app.lexico.ui.classic.BOTS
import app.lexico.ui.classic.OpponentPhoto
import app.lexico.ui.games.FinishedFoldersScreen
import app.lexico.ui.games.FinishedGamesScreen
import app.lexico.ui.games.InProgressScreen
import app.lexico.ui.games.ReviewScreen
import app.lexico.ui.games.StatsScreen

@Composable
internal fun InProgressRoute(lexico: Lexico, nav: Navigator) {
  val saves = lexico.savedGames
  var games by remember { mutableStateOf(saves.list()) }
  InProgressScreen(
    games.map(::inProgressItem), onBack = nav::back,
    onOpen = { id -> games.find { it.id == id }?.let { nav.replace(Screen.Continue(it)) } },
    onDelete = { id -> games.find { it.id == id }?.let { saves.delete(it.id, it.mode) }; games = saves.list() },
  )
}

@Composable
internal fun ContinueRoute(screen: Screen.Continue, lexico: Lexico, nav: Navigator, themes: ThemeState) {
  WhenReady(rememberCreated(screen) { lexico.continueGame(screen.saved) }, "Abriendo la partida…") { game ->
    when (game) {
      is ClassicGame -> ClassicPlay(game, nav, themes)
      is DuplicateGame -> DuplicatePlay(game, nav, themes)
    }
  }
}

@Composable
internal fun FinishedRoute(lexico: Lexico, nav: Navigator) {
  WhenReady(rememberCreated(Screen.Finished) { lexico.finishedGames.list() }, "Buscando tus partidas…") { games ->
    FinishedFoldersScreen(games.groupingBy { folderOf(it.mode) }.eachCount(), onBack = nav::back) { nav.go(Screen.FinishedFolder(it)) }
  }
}

@Composable
internal fun FinishedFolderRoute(screen: Screen.FinishedFolder, lexico: Lexico, nav: Navigator) {
  WhenReady(rememberCreated(screen) { lexico.finishedGames.list() }, "Buscando tus partidas…") { all ->
    val games = all.filter { folderOf(it.mode) == screen.folder }
    FinishedGamesScreen(screen.folder, games.map(::finishedItem), onBack = nav::back) { path -> nav.go(Screen.Review(path)) }
  }
}

@Composable
internal fun StatsRoute(lexico: Lexico, settings: Settings, nav: Navigator) {
  val since = settings.statsSince
  WhenReady(rememberCreated(Screen.Stats) { lexico.finishedGames.list() }, "Calculando tus estadísticas…") { all ->
    val games = all.filter { it.finishedAt > since }
    StatsScreen(
      BOTS.map { it.alias }, photo = { OpponentPhoto(it, 22.dp) },
      pageFor = { statsPage(it, games, reset = since > 0) }, onReset = settings::resetStats, onBack = nav::back,
    )
  }
}

@Composable
internal fun ReviewRoute(screen: Screen.Review, lexico: Lexico, nav: Navigator, style: BoardStyle) {
  WhenReady(rememberCreated(screen) { lexico.finishedGames.open(screen.path) }, "Abriendo la partida…") { review ->
    ReviewScreen(reviewView(review), style, nav::back)
  }
}
