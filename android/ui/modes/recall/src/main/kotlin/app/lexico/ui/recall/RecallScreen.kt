package app.lexico.ui.recall

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.lexico.ui.board.BoardStyle
import app.lexico.ui.common.Header
import app.lexico.ui.common.Lives
import app.lexico.ui.common.RaceBar
import app.lexico.ui.common.RaceBoard
import app.lexico.ui.common.RaceTrack
import app.lexico.ui.common.RaceWaiting
import app.lexico.ui.common.TurnClock
import app.lexico.ui.common.rememberRaceLeft
import app.lexico.ui.common.RulesBarButton
import app.lexico.ui.common.ThemeButton

@Composable
fun RecallScreen(
  session: RecallSession, style: BoardStyle, onExit: () -> Unit, race: RaceBoard? = null, onNextRound: () -> Unit = session::nextRound,
  onTheme: (() -> Unit)? = null,
) {
  var rules by remember { mutableStateOf(false) }
  Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Header("¿Cuántas recuerdas?", onExit) {
      RulesBarButton { rules = true }
      if (onTheme != null) ThemeButton(onTheme)
    }
    race?.let { RaceTop(session, it) }
    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) { StageContent(session, style, race, onNextRound, onExit) }
    if (session.config.limited) Lives(session.lives, session.config.lives, "Recordadas", session.recalled, session.best)
    race?.takeIf { it.goal == 0 }?.let { RaceBar(it) }
  }
  if (rules) RecallRulesDialog { rules = false }
}

@Composable
private fun RaceTop(session: RecallSession, race: RaceBoard) {
  if (race.goal > 0) RaceTrack(race)
  if (session.stage is Stage.Solving) rememberRaceLeft(race.endsAt)?.let { TurnClock(it) }
}

@Composable
private fun ColumnScope.StageContent(session: RecallSession, style: BoardStyle, race: RaceBoard?, onNextRound: () -> Unit, onExit: () -> Unit) {
  when (val stage = session.stage) {
    Stage.Loading -> race?.let { RaceWaiting(it, "Esperando a que todos estén listos") } ?: Loading()
    is Stage.Watching -> Watching(stage.game, session.config.intervalMs, style, session::watched)
    is Stage.Solving -> Solving(session, stage, style)
    Stage.RoundDone -> RoundDone(session, onNextRound, onExit)
    Stage.SeriesDone -> race?.let { RaceWaiting(it, "Sin vidas: esperando a los demás") } ?: SeriesDone(session, onExit)
    is Stage.Failed -> Failed(stage.message, session::retry)
  }
}

@Composable
private fun ColumnScope.Loading() {
  Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
}

@Composable
private fun Failed(message: String, onRetry: () -> Unit) {
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Text("No se pudo preparar la partida: $message", color = MaterialTheme.colorScheme.error)
    Button(onClick = onRetry) { Text("Reintentar") }
  }
}
