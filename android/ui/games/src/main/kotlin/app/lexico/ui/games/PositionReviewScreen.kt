package app.lexico.ui.games

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.lexico.ui.board.BoardStyle
import app.lexico.ui.common.BagDialog
import app.lexico.ui.common.BagIcon
import app.lexico.ui.common.Header

@Composable
fun PositionReviewScreen(review: ReviewView, style: BoardStyle, sessions: EndgameSessions, preEndgames: PreEndgameAnalyses, onBack: () -> Unit) {
  val turn = review.turns.first()
  var bag by remember { mutableStateOf(false) }
  Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
    Header(if (turn.endgame) "Análisis del final" else "Análisis del pre-final", onBack) { BagIcon(turn.bag) { bag = true } }
    BoxWithConstraints(Modifier.fillMaxWidth().weight(1f)) {
      val height = maxHeight
      Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) { TurnPage(review, 0, sessions, preEndgames, style, height) {} }
    }
  }
  if (bag) BagDialog("Por salir (bolsa ${turn.bag} + atril del rival)", turn.unseen) { bag = false }
}
