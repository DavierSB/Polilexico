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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.lexico.ui.board.BoardStyle
import app.lexico.ui.common.Header
import app.lexico.ui.common.Lives
import app.lexico.ui.common.ThemeButton

@Composable
fun RecallScreen(session: RecallSession, style: BoardStyle, onExit: () -> Unit, onTheme: (() -> Unit)? = null) {
  Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Header("¿Cuántas recuerdas?", onExit) {
      if (onTheme != null) ThemeButton(onTheme)
    }
    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) { StageContent(session, style, onExit) }
    Lives(session.lives, session.config.lives, "Recordadas", session.recalled, session.best)
  }
}

@Composable
private fun ColumnScope.StageContent(session: RecallSession, style: BoardStyle, onExit: () -> Unit) {
  when (val stage = session.stage) {
    Stage.Loading -> Loading()
    is Stage.Watching -> Watching(stage.game, session.config.intervalMs, style, session::watched)
    is Stage.Solving -> Solving(session, stage, style)
    Stage.RoundDone -> RoundDone(session, style, onExit)
    Stage.SeriesDone -> SeriesDone(session, style, onExit)
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
