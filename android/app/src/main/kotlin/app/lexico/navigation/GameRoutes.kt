package app.lexico.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import app.lexico.game.Lexico
import app.lexico.game.modes.classic.ClassicGame
import app.lexico.game.modes.duplicate.DuplicateGame
import app.lexico.game.modes.sprint.SprintGame
import app.lexico.game.modes.sprint.SprintPhase
import app.lexico.game.modes.sprint.SprintSetup
import app.lexico.game.records.SprintRecords
import app.lexico.menu.ThemeState
import app.lexico.modes.classic.ClassicController
import app.lexico.modes.classic.classicView
import app.lexico.modes.classic.toSetup
import app.lexico.modes.duplicate.DuplicateController
import app.lexico.modes.duplicate.duplicateView
import app.lexico.modes.duplicate.toSetup
import app.lexico.modes.recall.engineGames
import app.lexico.modes.sprint.SprintController
import app.lexico.modes.sprint.sprintView
import app.lexico.modes.sprint.toSetup
import app.lexico.ui.board.AppTheme
import app.lexico.ui.board.BoardStyle
import app.lexico.ui.classic.bot
import app.lexico.ui.classic.ClassicScreen
import app.lexico.ui.duplicate.DuplicateScreen
import app.lexico.ui.recall.RecallScreen
import app.lexico.ui.recall.RecallSession
import app.lexico.ui.sprint.SprintScreen

/*
 * Las pantallas de las partidas: crean la partida en el juego y la muestran mientras dura.
 */

@Composable
internal fun ClassicRoute(screen: Screen.Classic, lexico: Lexico, nav: Navigator, themes: ThemeState) {
  WhenReady(rememberCreated(screen) { lexico.newClassic(screen.config.toSetup()) }, "Preparando la partida…") {
    ClassicPlay(it, nav, themes)
  }
}

/** Finales: mientras HastyBot busca el final, espera; al salir antes, la busqueda se detiene. */
@Composable
internal fun EndgameRoute(screen: Screen.Endgame, lexico: Lexico, nav: Navigator, themes: ThemeState) {
  WhenReady(rememberCreated(screen) { lexico.newEndgame(screen.config.toSetup()) }, "Buscando un final…") {
    ClassicPlay(it, nav, themes)
  }
}

@Composable
internal fun DuplicateRoute(screen: Screen.Duplicate, lexico: Lexico, nav: Navigator, themes: ThemeState) {
  WhenReady(rememberCreated(screen) { lexico.newDuplicate(screen.config.toSetup()) }, "Preparando la partida…") {
    DuplicatePlay(it, nav, themes)
  }
}

@Composable
internal fun RecallRoute(screen: Screen.Recall, lexico: Lexico, nav: Navigator, themes: ThemeState) {
  val scope = rememberCoroutineScope()
  val session = remember(screen) { RecallSession(screen.config, engineGames(lexico), scope) }
  GameTheme(session, themes, themes.appTheme)
  RecallScreen(session, themes.current.board, nav::back) { themes.picking = true }
}

@Composable
internal fun SprintRoute(screen: Screen.Sprint, lexico: Lexico, nav: Navigator, themes: ThemeState) {
  WhenReady(rememberCreated(screen) { lexico.newSprint(screen.config.toSetup()) }, "Preparando la serie…") {
    SprintPlay(it, nav, themes, lexico.sprintRecords, screen.config.toSetup()) { nav.replace(screen.copy(run = screen.run + 1)) }
  }
}

@Composable
internal fun ClassicPlay(game: ClassicGame, nav: Navigator, themes: ThemeState) {
  PauseWhenHidden(game)
  GameTheme(game, themes, bot(game.opponent).theme)
  val scope = rememberCoroutineScope()
  val controller = remember(game) { ClassicController(game, scope, nav::back) { nav.replace(Screen.Review(it)) } }
  val state by game.state.collectAsState()
  ClassicScreen(classicView(state, game.opponent, controller.notice), themes.current.board, controller) { themes.picking = true }
}

@Composable
internal fun DuplicatePlay(game: DuplicateGame, nav: Navigator, themes: ThemeState) {
  PauseWhenHidden(game)
  GameTheme(game, themes, themes.appTheme)
  val scope = rememberCoroutineScope()
  val controller = remember(game) { DuplicateController(game, scope, nav::back) { nav.replace(Screen.Review(it)) } }
  val state by game.state.collectAsState()
  DuplicateScreen(duplicateView(state, controller.notice), themes.current.board, controller) { themes.picking = true }
}

@Composable
internal fun SprintPlay(
  game: SprintGame, nav: Navigator, themes: ThemeState, records: SprintRecords, setup: SprintSetup, onRestart: () -> Unit,
) {
  PauseWhenHidden(game)
  GameTheme(game, themes, themes.appTheme)
  val scope = rememberCoroutineScope()
  val controller = remember(game) { SprintController(game, scope, nav::back, onRestart) }
  val state by game.state.collectAsState()
  val finished = state.phase is SprintPhase.Finished
  val record = remember(finished) { if (finished) records.submit(setup, state.solved) else null }
  SprintScreen(sprintView(state, controller.notice, record), themes.current.board, controller) { themes.picking = true }
}

/**
 * Mientras la partida esta en pantalla, su tema: empieza en `initial` (el de su rival, o el de la
 * app) y el boton "Tema" lo cambia solo para ella. Al salir, vuelve el de la app.
 */
@Composable
private fun GameTheme(game: Any, themes: ThemeState, initial: AppTheme) {
  DisposableEffect(game) {
    themes.game = initial
    onDispose { themes.game = null }
  }
}
