package app.lexico.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import app.lexico.game.Cue
import app.lexico.game.Lexico
import app.lexico.game.modes.classic.ClassicGame
import app.lexico.game.modes.duplicate.DuplicateGame
import app.lexico.game.modes.duplicate.DuplicatePhase
import app.lexico.game.modes.sprint.Bingo
import app.lexico.game.modes.sprint.SprintGame
import app.lexico.game.modes.sprint.SprintPhase
import app.lexico.game.modes.sprint.SprintSetup
import app.lexico.game.modes.sprint.SprintState
import app.lexico.game.records.DuplicateRecords
import app.lexico.game.records.FinishedGames
import app.lexico.game.records.SprintRecords
import app.lexico.games.export.GameExport
import app.lexico.menu.ThemeState
import app.lexico.modes.anagram.anagramRecords
import app.lexico.modes.anagram.engineAnagramSource
import app.lexico.modes.classic.ClassicController
import app.lexico.modes.classic.classicView
import app.lexico.modes.classic.toSetup
import app.lexico.modes.duplicate.DuplicateController
import app.lexico.modes.duplicate.duplicateView
import app.lexico.modes.duplicate.efficiencyTenths
import app.lexico.modes.duplicate.toSetup
import app.lexico.modes.recall.engineGames
import app.lexico.modes.recall.recallRecords
import app.lexico.modes.sprint.SprintController
import app.lexico.modes.sprint.solvedAnswer
import app.lexico.modes.sprint.sprintView
import app.lexico.modes.sprint.toSetup
import app.lexico.sound.CelebrateRecord
import app.lexico.sound.PlayCues
import app.lexico.sound.recordBroken
import app.lexico.ui.anagram.AnagramScreen
import app.lexico.ui.anagram.AnagramSession
import app.lexico.ui.board.AppTheme
import app.lexico.ui.board.BoardStyle
import app.lexico.ui.common.LocalWordLookup
import app.lexico.ui.classic.ClassicScreen
import app.lexico.ui.classic.bot
import app.lexico.ui.duplicate.DuplicateScreen
import app.lexico.ui.recall.RecallScreen
import app.lexico.ui.recall.RecallSession
import app.lexico.ui.recall.Stage
import app.lexico.ui.sprint.SprintScreen

@Composable
internal fun ClassicRoute(screen: Screen.Classic, lexico: Lexico, nav: Navigator, themes: ThemeState) {
  WhenReady(rememberCreated(screen) { lexico.newClassic(screen.config.toSetup()) }, "Preparando la partida…") {
    ClassicPlay(it, nav, themes, lexico.finishedGames)
  }
}

@Composable
internal fun EndgameRoute(screen: Screen.Endgame, lexico: Lexico, nav: Navigator, themes: ThemeState) {
  WhenReady(rememberCreated(screen) { lexico.newEndgame(screen.config.toSetup()) }, "Buscando un final…") {
    ClassicPlay(it, nav, themes, lexico.finishedGames)
  }
}

@Composable
internal fun DuplicateRoute(screen: Screen.Duplicate, lexico: Lexico, nav: Navigator, themes: ThemeState) {
  WhenReady(rememberCreated(screen) { lexico.newDuplicate(screen.config.toSetup()) }, "Preparando la partida…") {
    DuplicatePlay(it, nav, themes, lexico.duplicateRecords, lexico.finishedGames)
  }
}

@Composable
internal fun RecallRoute(screen: Screen.Recall, lexico: Lexico, nav: Navigator, themes: ThemeState) {
  val scope = rememberCoroutineScope()
  val session = remember(screen) { RecallSession(screen.config, engineGames(lexico), recallRecords(lexico.recallRecords, screen.config), scope) }
  GameTheme(session, themes, themes.appTheme)
  PlayCues(session)
  LookupOnlyWhenOver(session.stage == Stage.RoundDone || session.stage == Stage.SeriesDone) {
    RecallScreen(session, themes.board, nav::back) { themes.picking = true }
  }
}

@Composable
internal fun AnagramRoute(screen: Screen.Anagram, lexico: Lexico, nav: Navigator, themes: ThemeState) {
  val scope = rememberCoroutineScope()
  val session = remember(screen) {
    AnagramSession(screen.config, engineAnagramSource(lexico), anagramRecords(lexico.anagramRecords, screen.config), scope)
  }
  LifecycleEventEffect(Lifecycle.Event.ON_STOP) { session.pause() }
  GameTheme(session, themes, themes.appTheme)
  PlayCues(session)
  AnagramScreen(session, themes.board, nav::back) { themes.picking = true }
}

@Composable
internal fun SprintRoute(screen: Screen.Sprint, lexico: Lexico, nav: Navigator, themes: ThemeState) {
  WhenReady(rememberCreated(screen) { lexico.newSprint(screen.config.toSetup()) }, "Preparando la serie…") {
    SprintPlay(it, nav, themes, lexico.sprintRecords, screen.config.toSetup()) { nav.replace(screen.copy(run = screen.run + 1)) }
  }
}

@Composable
internal fun ClassicPlay(
  game: ClassicGame, nav: Navigator, themes: ThemeState, finished: FinishedGames, wifiNotice: String? = null, handOff: Boolean = true,
) {
  PauseWhenHidden(game)
  PlayCues(game) { it.takeIf { cue -> cue != Cue.CELEBRATION } }
  GameTheme(game, themes, if (game.host == null) bot(game.opponent).theme else themes.appTheme)
  val scope = rememberCoroutineScope()
  var exporting by remember { mutableStateOf<String?>(null) }
  val controller = remember(game) {
    ClassicController(game, scope, nav::back, { exporting = it }) { path, atEndgame -> nav.replace(Screen.Review(path, atEndgame)) }
  }
  val state by game.state.collectAsState()
  LookupOnlyWhenOver(state.result != null || game.study) {
    val view = classicView(state, game.opponent, controller.notice, game.host, handOff && game.host != null).copy(status = wifiNotice)
    ClassicScreen(view, themes.board, controller) { themes.picking = true }
  }
  exporting?.let { GameExport(it, finished, themes.board) { exporting = null } }
}

@Composable
internal fun DuplicatePlay(game: DuplicateGame, nav: Navigator, themes: ThemeState, records: DuplicateRecords, games: FinishedGames) {
  PauseWhenHidden(game)
  val best = remember(game) { records.best(game.setup) }
  PlayCues(game) { if (it == Cue.GAME_OVER && efficiencyTenths(game.state.value) > best) Cue.CELEBRATION else it }
  GameTheme(game, themes, themes.appTheme)
  val scope = rememberCoroutineScope()
  var exporting by remember { mutableStateOf<String?>(null) }
  val controller = remember(game) { DuplicateController(game, scope, nav::back, { exporting = it }) { nav.replace(Screen.Review(it)) } }
  val state by game.state.collectAsState()
  val finished = state.phase is DuplicatePhase.Finished
  remember(finished) { if (finished) records.submit(game.setup, efficiencyTenths(state)) }
  LookupOnlyWhenOver(finished || game.setup.study) { DuplicateScreen(duplicateView(state, controller.notice), themes.board, controller) { themes.picking = true } }
  exporting?.let { GameExport(it, games, themes.board) { exporting = null } }
}

@Composable
internal fun SprintPlay(
  game: SprintGame, nav: Navigator, themes: ThemeState, records: SprintRecords, setup: SprintSetup, onRestart: () -> Unit,
) {
  PauseWhenHidden(game)
  PlayCues(game)
  GameTheme(game, themes, themes.appTheme)
  val scope = rememberCoroutineScope()
  val controller = remember(game) { SprintController(game, scope, nav::back, onRestart) }
  val state by game.state.collectAsState()
  val finished = state.phase is SprintPhase.Finished
  val best = remember(game) { records.best(setup) }
  val record = remember(finished) { if (finished) records.submit(setup, state.solved) else null }
  CelebrateRecord(recordBroken(state.solved, best, record?.isNew == true))
  val played = rememberPlayed(game, state)
  LookupOnlyWhenOver(state.phase is SprintPhase.Revealed || finished) {
    SprintScreen(sprintView(state, controller.notice, best, record, played), themes.board, controller) { themes.picking = true }
  }
}

@Composable
internal fun rememberPlayed(game: SprintGame, state: SprintState): List<Bingo> {
  val played = remember(game) { mutableStateMapOf<Int, Bingo>() }
  LaunchedEffect(state.phase) { solvedAnswer(state.phase)?.let { played[state.posed] = it } }
  return played.toSortedMap().values.toList()
}

@Composable
internal fun GameTheme(game: Any, themes: ThemeState, initial: AppTheme) {
  DisposableEffect(game) {
    themes.game = initial
    onDispose { themes.game = null }
  }
}

@Composable
internal fun LookupOnlyWhenOver(over: Boolean, content: @Composable () -> Unit) {
  if (over) content() else CompositionLocalProvider(LocalWordLookup provides null, content = content)
}
