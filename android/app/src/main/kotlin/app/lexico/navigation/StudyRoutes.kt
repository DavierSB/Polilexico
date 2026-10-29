package app.lexico.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import app.lexico.game.Lexico
import app.lexico.games.endgameSession
import app.lexico.games.positionView
import app.lexico.games.preEndgameRanking
import app.lexico.menu.ThemeState
import app.lexico.modes.analysis.studyPosition
import app.lexico.ui.games.EndgameSessions
import app.lexico.ui.games.PositionReviewScreen
import app.lexico.ui.games.PreEndgameAnalyses

@Composable
internal fun PositionReviewRoute(screen: Screen.PositionReview, lexico: Lexico, nav: Navigator, themes: ThemeState) {
  val position = remember(screen) { studyPosition(screen.position) }
  WhenReady(rememberCreated(screen) { lexico.studies.turn(position) }, "Preparando el análisis…") { turn ->
    val view = remember(turn) { positionView(turn) }
    val sessions: EndgameSessions = remember(view) { { endgameSession(lexico.studies.explore(position), view.turns[0], view.opponent) } }
    val preEndgames: PreEndgameAnalyses = remember(view) { { _, update -> preEndgameRanking(lexico.studies.preEndgame(position), view.turns[0], update) } }
    PositionReviewScreen(view, themes.board, sessions, preEndgames, nav::back)
  }
}
