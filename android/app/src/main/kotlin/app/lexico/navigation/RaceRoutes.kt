package app.lexico.navigation

import android.os.SystemClock
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import app.lexico.game.Lexico
import app.lexico.game.modes.sprint.SprintGame
import app.lexico.game.modes.sprint.SprintPhase
import app.lexico.game.race.Race
import app.lexico.game.race.RaceGame
import app.lexico.game.race.RaceSetup
import app.lexico.game.race.RaceView
import app.lexico.menu.ThemeState
import app.lexico.modes.anagram.engineAnagramSource
import app.lexico.modes.anagram.toAnagramConfig
import app.lexico.modes.recall.sharedGames
import app.lexico.modes.recall.toRecallConfig
import app.lexico.modes.sprint.SprintController
import app.lexico.modes.sprint.sprintView
import app.lexico.modes.sprint.toSprintSetup
import app.lexico.sound.PlayCues
import app.lexico.ui.anagram.AnagramScreen
import app.lexico.ui.anagram.AnagramSession
import app.lexico.ui.anagram.Stage as AnagramStage
import app.lexico.ui.common.GuestLobbyScreen
import app.lexico.ui.common.HostLobbyScreen
import app.lexico.ui.common.Notice
import app.lexico.ui.common.RaceBoard
import app.lexico.ui.common.RaceResultsScreen
import app.lexico.ui.common.RaceRunner
import app.lexico.ui.recall.RecallScreen
import app.lexico.ui.recall.RecallSession
import app.lexico.ui.recall.Record
import app.lexico.ui.recall.RecordBook
import app.lexico.ui.recall.Stage
import app.lexico.ui.sprint.SprintScreen
import app.lexico.wifi.RaceRoom
import app.lexico.wifi.RaceSeat
import app.lexico.wifi.Room
import app.lexico.wifi.RoomKind

private const val SESSION_MS = 24 * 60 * 60_000L

private typealias Report = (score: Int, points: Int, done: Boolean, lives: Int) -> Unit

private interface Rounds {
  fun finish(round: Int)
  fun ready()
}

@Composable
internal fun RaceHostRoute(screen: Screen.RaceHost, lexico: Lexico, nav: Navigator, themes: ThemeState) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val room = remember(screen) {
    val games = if (screen.setup.game == RaceGame.RECALL) suspend { lexico.demoGames.next() } else null
    RaceRoom(context, screen.name, kindOf(screen.setup.game), screen.setup, scope, games)
  }
  val error = remember(room) { room.open() }
  DisposableEffect(room) { onDispose { room.close() } }
  val race by room.race.collectAsState()
  val players by room.players.collectAsState()
  race?.let { HostRace(it, screen.name, lexico, nav, themes) }
    ?: HostLobbyScreen(room.name, players, error, room::begin, nav::back)
}

@Composable
internal fun RaceJoinRoute(screen: Screen.RaceJoin, lexico: Lexico, nav: Navigator, themes: ThemeState) {
  var room by remember { mutableStateOf<Room?>(null) }
  var refusal by remember { mutableStateOf<String?>(null) }
  val chosen = room
  if (chosen == null) FindRooms(kindOf(screen.game), refusal, nav) { room = it; refusal = null }
  else GuestRace(screen.name, chosen, lexico, nav, themes) { refusal = it; room = null }
}

@Composable
private fun HostRace(race: Race, me: String, lexico: Lexico, nav: Navigator, themes: ThemeState) {
  val view by race.view.collectAsState()
  val rounds = remember(race) {
    object : Rounds {
      override fun finish(round: Int) = race.finishRound(me, round)
      override fun ready() = race.ready(me)
    }
  }
  RacePlay(view, me, null, lexico, nav, themes, rounds) { score, points, done, lives -> race.report(me, score, points, done, lives) }
}

@Composable
private fun GuestRace(name: String, room: Room, lexico: Lexico, nav: Navigator, themes: ThemeState, onRefused: (String) -> Unit) {
  val scope = rememberCoroutineScope()
  val seat = remember(room) { RaceSeat(name, room, scope) }
  DisposableEffect(seat) { seat.open(); onDispose { seat.close() } }
  val view by seat.view.collectAsState()
  val players by seat.players.collectAsState()
  val status by seat.status.collectAsState()
  val refusal by seat.refusal.collectAsState()
  LaunchedEffect(refusal) { refusal?.let(onRefused) }
  val rounds = remember(seat) {
    object : Rounds {
      override fun finish(round: Int) = seat.finishRound(round)
      override fun ready() = seat.ready()
    }
  }
  view?.let { RacePlay(it, name, status, lexico, nav, themes, rounds, seat::report) }
    ?: GuestLobbyScreen(room.name, players, status, name) { onRefused("") }
}

@Composable
private fun RacePlay(
  view: RaceView, me: String, status: String?, lexico: Lexico, nav: Navigator, themes: ThemeState, rounds: Rounds, report: Report,
) {
  val board = rememberBoard(view, me)
  if (view.over) return RaceResultsScreen(board, nav::back)
  Column(Modifier.fillMaxSize()) {
    status?.let { Notice(it, bold = true) }
    Box(Modifier.weight(1f)) { RaceGameScreen(view, board, lexico, nav, themes, rounds, report) }
  }
}

@Composable
private fun RaceGameScreen(view: RaceView, board: RaceBoard, lexico: Lexico, nav: Navigator, themes: ThemeState, rounds: Rounds, report: Report) {
  when (view.setup.game) {
    RaceGame.ANAGRAM -> AnagramRace(view.setup, board, lexico, nav, themes, report)
    RaceGame.SPRINT -> SprintRace(view.setup, board, lexico, nav, themes, report)
    RaceGame.RECALL -> RecallRace(view, board, lexico, nav, themes, rounds, report)
  }
}

@Composable
private fun AnagramRace(setup: RaceSetup, board: RaceBoard, lexico: Lexico, nav: Navigator, themes: ThemeState, report: Report) {
  val scope = rememberCoroutineScope()
  val session = remember { AnagramSession(setup.toAnagramConfig(SESSION_MS), engineAnagramSource(lexico), NoRecords, scope) }
  val done = session.stage is AnagramStage.Finished
  LaunchedEffect(session.solved, done, session.lives) { report(session.solved, 0, done, session.lives) }
  GameTheme(session, themes, themes.appTheme)
  PlayCues(session)
  AnagramScreen(session, themes.board, nav::back, board) { themes.picking = true }
}

@Composable
private fun SprintRace(setup: RaceSetup, board: RaceBoard, lexico: Lexico, nav: Navigator, themes: ThemeState, report: Report) {
  WhenReady(rememberCreated(setup) { lexico.newSprint(setup.toSprintSetup(SESSION_MS)) }, "Preparando la serie…") {
    SprintRacePlay(it, board, nav, themes, report)
  }
}

@Composable
private fun SprintRacePlay(game: SprintGame, board: RaceBoard, nav: Navigator, themes: ThemeState, report: Report) {
  DisposableEffect(game) { onDispose { game.close() } }
  PlayCues(game)
  GameTheme(game, themes, themes.appTheme)
  val scope = rememberCoroutineScope()
  val controller = remember(game) { SprintController(game, scope, nav::back) {} }
  val state by game.state.collectAsState()
  val done = state.phase is SprintPhase.Finished
  LaunchedEffect(state.solved, done, state.lives) { report(state.solved, 0, done, state.lives) }
  val played = rememberPlayed(game, state)
  LookupOnlyWhenOver(state.phase is SprintPhase.Revealed || done) {
    SprintScreen(sprintView(state, controller.notice, 0, null, played).copy(race = board), themes.board, controller) { themes.picking = true }
  }
}

@Composable
private fun RecallRace(view: RaceView, board: RaceBoard, lexico: Lexico, nav: Navigator, themes: ThemeState, rounds: Rounds, report: Report) {
  val scope = rememberCoroutineScope()
  val games = remember { sharedGames(lexico) }
  val session = remember { RecallSession(view.setup.toRecallConfig(), games, NoRecords, scope) }
  val round = view.round
  LaunchedEffect(round?.number) { round?.let { games.offer(it.number, it.game) } }
  LaunchedEffect(round) { if (round?.open == false && round.number == session.round) session.closeRound() }
  val out = session.stage == Stage.RoundDone && session.lives == 0
  LaunchedEffect(session.recalled, session.points, session.lives, out) { report(session.recalled, session.points, out, session.lives) }
  LaunchedEffect(session.stage) { if (session.answeredAll && session.stage != Stage.Loading) rounds.finish(session.round) }
  GameTheme(session, themes, themes.appTheme)
  PlayCues(session)
  LookupOnlyWhenOver(session.stage == Stage.RoundDone || session.stage == Stage.SeriesDone) {
    RecallScreen(session, themes.board, nav::back, board, onNextRound = { rounds.ready(); session.nextRound() }) { themes.picking = true }
  }
}

@Composable
private fun rememberBoard(view: RaceView, me: String): RaceBoard = remember(view, me) {
  RaceBoard(
    me, view.runners.map { RaceRunner(it.name, it.score, it.done, it.present, it.lives, it.ready) }, view.setup.goal,
    (view.remainingMs ?: view.round?.takeIf { it.open }?.remainingMs)?.let { SystemClock.elapsedRealtime() + it }, unitOf(view.setup.game), "${unitOf(view.setup.game)}s",
  )
}

private fun unitOf(game: RaceGame): String = when (game) {
  RaceGame.ANAGRAM -> "anagrama"
  RaceGame.SPRINT -> "scrabble"
  RaceGame.RECALL -> "palabra"
}

private fun kindOf(game: RaceGame): RoomKind = when (game) {
  RaceGame.ANAGRAM -> RoomKind.ANAGRAM
  RaceGame.SPRINT -> RoomKind.SPRINT
  RaceGame.RECALL -> RoomKind.RECALL
}

private object NoRecords : RecordBook {
  override fun best(): Int = 0

  override fun submit(recalled: Int): Record = Record(0, false)
}
