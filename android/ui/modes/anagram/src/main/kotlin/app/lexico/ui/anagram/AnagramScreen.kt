package app.lexico.ui.anagram

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import app.lexico.ui.common.BarTitle
import app.lexico.ui.common.ExitButton
import app.lexico.ui.common.Lives
import app.lexico.ui.common.Pausable
import app.lexico.ui.common.PauseButton
import app.lexico.ui.common.RaceBar
import app.lexico.ui.common.RaceBoard
import app.lexico.ui.common.RaceTrack
import app.lexico.ui.common.RaceWaiting
import app.lexico.ui.common.RulesBarButton
import app.lexico.ui.common.ThemeButton
import app.lexico.ui.common.TurnClock
import app.lexico.ui.common.rememberRaceLeft

@Composable
fun AnagramScreen(session: AnagramSession, style: BoardStyle, onExit: () -> Unit, race: RaceBoard? = null, onTheme: (() -> Unit)? = null) {
  Pausable(session.paused, session::resume) {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      TopBar(session, onExit, race == null, onTheme)
      TurnClock(race?.let { rememberRaceLeft(it.endsAt) } ?: session.remainingMs)
      race?.takeIf { it.goal > 0 }?.let { RaceTrack(it) }
      Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) { StageContent(session, style, race, onExit) }
      Lives(session.lives, session.config.lives, "Resueltas", session.solved, session.best)
      race?.takeIf { it.goal == 0 }?.let { RaceBar(it) }
    }
  }
}

@Composable
private fun TopBar(session: AnagramSession, onExit: () -> Unit, pausable: Boolean, onTheme: (() -> Unit)?) {
  var rules by remember { mutableStateOf(false) }
  Row(Modifier.fillMaxWidth().height(44.dp), Arrangement.spacedBy(6.dp), Alignment.CenterVertically) {
    ExitButton(onExit)
    BarTitle("Anagrama", Modifier.weight(1f).padding(start = 6.dp))
    if (pausable && session.stage is Stage.Solving) PauseButton(session::pause)
    RulesBarButton { rules = true }
    if (onTheme != null) ThemeButton(onTheme)
  }
  if (rules) AnagramRulesDialog { rules = false }
}

@Composable
private fun ColumnScope.StageContent(session: AnagramSession, style: BoardStyle, race: RaceBoard?, onExit: () -> Unit) {
  when (val stage = session.stage) {
    Stage.Loading -> Loading()
    is Stage.Solving -> Solving(session, stage, style)
    is Stage.Revealed -> Revealed(session, stage)
    is Stage.Finished -> race?.let { RaceWaiting(it, "Sin vidas: esperando a los demás") } ?: Finished(session, stage, onExit)
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
    Text("No se pudo preparar la mano: $message", color = MaterialTheme.colorScheme.error)
    Button(onClick = onRetry) { Text("Reintentar") }
  }
}
