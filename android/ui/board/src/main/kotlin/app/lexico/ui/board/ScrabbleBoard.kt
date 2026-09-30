package app.lexico.ui.board

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.rememberTextMeasurer
import app.lexico.model.Board
import app.lexico.model.Position
import app.lexico.model.Tile

@Composable
fun ScrabbleBoard(
  board: Board,
  modifier: Modifier = Modifier,
  style: BoardStyle = BoardStyles.Isc,
  pending: Map<Position, Tile> = emptyMap(),
  arrow: Arrow? = null,
  latestScore: Int? = null,
  showScore: Boolean = false,
  liveScore: LiveScore? = null,
  onTap: ((Position) -> Unit)? = null,
) {
  val measurer = rememberTextMeasurer(cacheSize = 64)
  val popup = rememberScorePopup(board, latestScore.takeIf { showScore })
  Canvas(modifier.aspectRatio(1f).then(tapsOn(style, onTap))) {
    with(BoardPainter(style, measurer, BoardGeometry(size.width, style.showCoordinates))) {
      drawBoard(board, pending, arrow)
      liveScore?.let { drawLiveScore(it) }
      popup?.let { drawScore(it) }
    }
  }
}

@Composable
fun ScrabbleBoard(
  state: BoardState,
  modifier: Modifier = Modifier,
  style: BoardStyle = BoardStyles.Night,
  showArrow: Boolean = true,
  latestScore: Int? = null,
  showScore: Boolean = false,
  onTap: ((Position) -> Unit)? = null,
) = ScrabbleBoard(
  state.board, modifier, style, state.pending, if (showArrow) state.arrow else null, latestScore, showScore, onTap = onTap,
)

private fun tapsOn(style: BoardStyle, onTap: ((Position) -> Unit)?): Modifier =
  if (onTap == null) Modifier else Modifier.pointerInput(onTap, style.showCoordinates) {
    detectTapGestures { offset ->
      BoardGeometry(size.width.toFloat(), style.showCoordinates).positionAt(offset)?.let(onTap)
    }
  }
