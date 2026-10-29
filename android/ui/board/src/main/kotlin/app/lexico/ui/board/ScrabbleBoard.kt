package app.lexico.ui.board

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.rememberTextMeasurer
import app.lexico.model.Board
import app.lexico.model.Position
import app.lexico.model.Tile
import app.lexico.ui.common.LocalWordLookup
import app.lexico.ui.common.awaitLongPress

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
  wordTap: WordTap = WordTap.NONE,
  landing: Boolean = false,
  onTap: ((Position) -> Unit)? = null,
) {
  val measurer = rememberTextMeasurer(cacheSize = 64)
  val landed = rememberLanding(board, landing)
  val popup = rememberScorePopup(board, latestScore.takeIf { showScore }, landed?.durationMs ?: 0)
  val lookup = LocalWordLookup.current
  var choices by remember { mutableStateOf<WordChoices?>(null) }
  val current by rememberUpdatedState(board)
  val haptic = LocalHapticFeedback.current
  val words: WordsAt? = remember(lookup) {
    lookup?.let { open ->
      { at: Offset, p: Position, direct: Boolean ->
        choices = offer(wordsAt(current, p), at, direct, open)
        if (choices != null) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
      }
    }
  }
  val taps = tapsFor(wordTap, words, onTap, { current })
  val presses = words?.takeIf { wordTap != WordTap.OPEN }
  Box(modifier.aspectRatio(1f)) {
    Canvas(Modifier.fillMaxSize().then(tapsOn(style, taps)).then(longPresses(style, presses))) {
      with(BoardPainter(style, measurer, BoardGeometry(size.width, style.showCoordinates))) {
        drawBoard(board, pending, arrow, landed)
        liveScore?.let { drawLiveScore(it) }
        popup?.let { drawScore(it) }
      }
    }
    WordMenu(choices, { lookup?.invoke(it) }) { choices = null }
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

enum class WordTap { NONE, OPEN, MENU }

private typealias WordsAt = (Offset, Position, Boolean) -> Unit

private fun tapsFor(wordTap: WordTap, words: WordsAt?, onTap: ((Position) -> Unit)?, board: () -> Board): ((Position, Offset) -> Unit)? {
  if (wordTap == WordTap.NONE || words == null) return onTap?.let { t -> { p, _ -> t(p) } }
  return { p, at ->
    if (board()[p] != null) words(at, p, wordTap == WordTap.OPEN) else onTap?.invoke(p)
  }
}

private fun offer(words: List<String>, at: Offset, direct: Boolean, open: (String) -> Unit): WordChoices? {
  if (direct && words.size == 1) open(words.single())
  return if (words.isEmpty() || (direct && words.size == 1)) null else WordChoices(at, words)
}

private fun tapsOn(style: BoardStyle, onTap: ((Position, Offset) -> Unit)?): Modifier =
  if (onTap == null) Modifier else Modifier.pointerInput(onTap, style.showCoordinates) {
    detectTapGestures { offset ->
      BoardGeometry(size.width.toFloat(), style.showCoordinates).positionAt(offset)?.let { onTap(it, menuAt(offset)) }
    }
  }

private fun longPresses(style: BoardStyle, words: WordsAt?): Modifier =
  if (words == null) Modifier else Modifier.pointerInput(words, style.showCoordinates) {
    awaitLongPress { offset ->
      BoardGeometry(size.width.toFloat(), style.showCoordinates).positionAt(offset)?.let { words(menuAt(offset), it, false) }
    }
  }

private fun PointerInputScope.menuAt(offset: Offset): Offset = Offset(offset.x, offset.y - size.height)
