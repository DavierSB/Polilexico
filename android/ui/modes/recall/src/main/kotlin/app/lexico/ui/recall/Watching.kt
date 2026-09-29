package app.lexico.ui.recall

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import app.lexico.model.Board
import app.lexico.ui.board.BoardStyle
import app.lexico.ui.board.ScrabbleBoard
import kotlinx.coroutines.delay

/** Pausa al terminar la partida, con el tablero completo, antes de quitarlo. */
private const val FINAL_PAUSE_MS = 2000L

/** La partida jugada a jugada, con los puntos de cada una; al terminar, [onDone]. */
@Composable
internal fun Watching(game: List<ScoredPlacement>, intervalMs: Long, style: BoardStyle, onDone: () -> Unit) {
  val replay = remember(game) { Replay() }
  LaunchedEffect(game) {
    replay.play(game, intervalMs)
    delay(FINAL_PAUSE_MS)
    onDone()
  }
  Text("Fíjate en las palabras que más puntos hacen.", style = MaterialTheme.typography.bodyMedium)
  ScrabbleBoard(replay.board, Modifier.fillMaxWidth(), style)
  Text("Jugada ${replay.shown} de ${game.size}", style = MaterialTheme.typography.labelMedium)
}

/** El tablero de la partida mientras se reproduce y cuantas jugadas van. */
@Stable
private class Replay {
  var board by mutableStateOf(Board.EMPTY)
    private set
  var shown by mutableIntStateOf(0)
    private set

  /** Pone las jugadas una a una, cada `intervalMs`. */
  suspend fun play(game: List<ScoredPlacement>, intervalMs: Long) {
    for ((placement, _) in game) {
      delay(intervalMs)
      board = runCatching { board.play(placement) }.getOrDefault(board)
      shown++
    }
  }
}
