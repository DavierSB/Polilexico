package app.lexico.home

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import app.lexico.model.Board
import app.lexico.ui.board.BoardStyle
import app.lexico.ui.board.ScrabbleBoard
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.delay

/**
 * Tablero de adorno para el inicio: partidas de verdad (las colocaciones en notacion FISE que da
 * `nextGame`) que se van jugando solas, una jugada cada poco. Mientras se ve una, ya se pide la
 * siguiente.
 */
@Composable
fun DemoBoard(nextGame: suspend () -> List<String>, style: BoardStyle, modifier: Modifier = Modifier) {
  var board by remember { mutableStateOf(Board.EMPTY) }
  LaunchedEffect(nextGame) {
    var next = fetch(nextGame)
    while (true) {
      val game = next.await()
      next = fetch(nextGame)
      replay(game) { board = it }
    }
  }
  ScrabbleBoard(board, modifier.fillMaxWidth(), style)
}

/** Pausa entre jugada y jugada, y al terminar antes de empezar de nuevo. */
private const val PAUSE_MS = 1600L
private const val FINAL_PAUSE_MS = 6000L

/** Pide una partida en segundo plano; si el motor falla, una vacia. */
private fun CoroutineScope.fetch(nextGame: suspend () -> List<String>): Deferred<List<String>> =
  async { runCatching { nextGame() }.getOrDefault(emptyList()) }

/** Juega la partida desde el tablero vacio, mostrando cada tablero con `show`. */
private suspend fun replay(game: List<String>, show: (Board) -> Unit) {
  var board = Board.EMPTY
  show(board)
  for (placement in game) {
    delay(PAUSE_MS)
    board = runCatching { board.play(placement) }.getOrDefault(board)
    show(board)
  }
  delay(FINAL_PAUSE_MS)
}
