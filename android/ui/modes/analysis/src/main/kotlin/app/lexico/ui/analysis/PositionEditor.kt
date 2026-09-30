package app.lexico.ui.analysis

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import app.lexico.model.Board
import app.lexico.model.Letters
import app.lexico.model.Position
import app.lexico.model.Tile
import app.lexico.ui.board.BoardState
import app.lexico.ui.common.RankedMove

@Composable
fun rememberPositionEditor(): PositionEditor = remember { PositionEditor() }

@Stable
class PositionEditor {
  val state = BoardState()
  val rack = mutableStateListOf<String>()
  var target by mutableStateOf(Target.BOARD)
    private set

  var letter: String? by mutableStateOf(null)
    private set

  var pendingBlank: Position? by mutableStateOf(null)
    private set
  private var blankFromArrow = false

  var version by mutableIntStateOf(0)
    private set

  fun tapSquare(p: Position) {
    val chosen = letter
    when {
      target == Target.RACK -> Unit
      state.board[p] != null -> update(state.board.withoutTile(p))
      chosen != null && state.arrow == null -> put(chosen, p, fromArrow = false)
      else -> state.toggleArrow(p)
    }
  }

  fun tapLetter(l: String) {
    if (target == Target.RACK) addToRack(l) else writeOnBoard(l)
  }

  fun placeBlank(l: String?) {
    val p = pendingBlank
    pendingBlank = null
    if (l == null || p == null) return
    update(state.board.withTile(p, Tile(l, blank = true)))
    if (blankFromArrow) state.advanceArrow()
  }

  fun selectTarget(t: Target) {
    target = t
    if (t == Target.RACK) letter = null
  }

  fun removeFromRack(i: Int) {
    rack.removeAt(i)
    version++
  }

  fun placeCandidate(c: RankedMove) {
    val placement = c.placement ?: return
    runCatching { state.board.play(placement) }.onSuccess(::update)
  }

  fun clear() {
    state.reset(Board.EMPTY)
    rack.clear()
    letter = null
    version++
  }

  private fun addToRack(l: String) {
    if (rack.size >= MAX_RACK) return
    rack.add(l)
    version++
  }

  private fun writeOnBoard(l: String) {
    val at = state.arrow?.position
    if (at != null) put(l, at, fromArrow = true) else letter = if (letter == l) null else l
  }

  private fun put(l: String, p: Position, fromArrow: Boolean) {
    if (l == Letters.BLANK) return askBlankLetter(p, fromArrow)
    update(state.board.withTile(p, Tile(l)))
    if (fromArrow) state.advanceArrow()
  }

  private fun askBlankLetter(p: Position, fromArrow: Boolean) {
    blankFromArrow = fromArrow
    pendingBlank = p
  }

  private fun update(board: Board) {
    state.board = board.withoutHighlight()
    version++
  }

  companion object {
    const val MAX_RACK = 7
  }
}
