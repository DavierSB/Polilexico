package app.lexico.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import app.lexico.game.Lexico
import app.lexico.games.export.GameExport
import app.lexico.ui.common.ExportKind
import app.lexico.game.modes.classic.ClassicGame
import app.lexico.game.modes.duplicate.DuplicateGame
import app.lexico.game.storage.Mode
import app.lexico.games.endgameSession
import app.lexico.games.finishedItem
import app.lexico.games.folderOf
import app.lexico.games.inProgressItem
import app.lexico.games.preEndgameRanking
import app.lexico.games.reviewView
import app.lexico.games.StatsGames
import app.lexico.games.statsPage
import app.lexico.menu.Settings
import app.lexico.menu.ThemeState
import app.lexico.ui.classic.BOTS
import app.lexico.ui.classic.bot
import app.lexico.ui.classic.OpponentPhoto
import app.lexico.ui.games.EndgameSessions
import app.lexico.ui.games.FinishedGamesScreen
import app.lexico.ui.games.InProgressScreen
import app.lexico.ui.games.PreEndgameAnalyses
import app.lexico.ui.games.ReviewScreen
import app.lexico.ui.games.StatsScreen

@Composable
internal fun InProgressRoute(screen: Screen.InProgress, lexico: Lexico, nav: Navigator) {
  val saves = lexico.savedGames
  val list = { saves.list().filter { it.mode == screen.mode } }
  var games by remember { mutableStateOf(list()) }
  InProgressScreen(
    inProgressTitle(screen.mode), games.map { inProgressItem(it, saves.progress(it)) }, onBack = nav::back,
    onOpen = { id -> games.find { it.id == id }?.let { nav.replace(Screen.Continue(it)) } },
    onDelete = { ids -> games.filter { it.id in ids }.forEach { saves.abandon(it.id, it.mode) }; games = list() },
  )
}

private fun inProgressTitle(mode: Mode): String = if (mode == Mode.DUPLICATE) "Duplicadas en curso" else "Clásicas en curso"

@Composable
internal fun ContinueRoute(screen: Screen.Continue, lexico: Lexico, nav: Navigator, themes: ThemeState) {
  WhenReady(rememberCreated(screen) { lexico.continueGame(screen.saved) }, "Abriendo la partida…") { game ->
    when (game) {
      is ClassicGame -> ClassicPlay(game, nav, themes, lexico.finishedGames)
      is DuplicateGame -> DuplicatePlay(game, nav, themes, lexico.duplicateRecords, lexico.finishedGames)
    }
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
  WhenReady(rememberCreated(Screen.Stats) { statsGames(lexico, since) }, "Calculando tus estadísticas…") { games ->
    StatsScreen(
      BOTS.map { it.alias }, photo = { OpponentPhoto(it, 22.dp) },
      pageFor = { statsPage(it, games, reset = since > 0) }, onReset = settings::resetStats, onBack = nav::back,
    )
  }
}

private suspend fun statsGames(lexico: Lexico, since: Long): StatsGames = StatsGames(
  lexico.finishedGames.list().filter { it.finishedAt > since },
  lexico.savedGames.abandonedGames().filter { it.at > since },
  lexico.savedGames.unfinished().filter { it.at > since },
)

@Composable
internal fun ReviewRoute(screen: Screen.Review, lexico: Lexico, nav: Navigator, themes: ThemeState) {
  WhenReady(rememberCreated(screen) { lexico.finishedGames.open(screen.path) }, "Abriendo la partida…") { review ->
    GameTheme(review, themes, if (review.game.mode == Mode.DUPLICATE) themes.appTheme else bot(review.game.opponent).theme)
    val view = remember(review) { reviewView(review) }
    val sessions: EndgameSessions = remember(view) { { i -> endgameSession(lexico.finishedGames.explore(screen.path, i), view.turns[i], view.opponent) } }
    val preEndgames: PreEndgameAnalyses = remember(view) {
      { i, update -> preEndgameRanking(lexico.finishedGames.preEndgame(screen.path, i), view.turns[i], update) }
    }
    var exporting by remember { mutableStateOf<ExportKind?>(null) }
    ReviewScreen(view, themes.board, sessions, preEndgames, screen.atEndgame, nav::back, { exporting = it }) { themes.picking = true }
    exporting?.let { GameExport(screen.path, lexico.finishedGames, themes.board, it) { exporting = null } }
  }
}
