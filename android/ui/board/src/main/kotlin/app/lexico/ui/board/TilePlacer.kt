package app.lexico.ui.board

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import app.lexico.model.Tile
import app.lexico.model.Placement
import app.lexico.model.Letters
import app.lexico.model.Position
import app.lexico.model.Board

@Composable
fun rememberTilePlacer(renewal: RackRenewal = RackRenewal.MY_PLAYS): TilePlacer = remember { TilePlacer(renewal) }

@Stable
class TilePlacer(private val renewal: RackRenewal = RackRenewal.MY_PLAYS) {
  val state = BoardState()

  var tiles: List<String> by mutableStateOf(emptyList())
    private set

  val order = mutableStateListOf<Int>()

  var selected: Int? by mutableStateOf(null)
    private set

  val placed = mutableStateMapOf<Position, Int>()

  var exchanging by mutableStateOf(false)
    private set

  val toExchange = mutableStateListOf<Int>()

  private var exchanged: List<Int> = emptyList()

  private var staying: List<String> = emptyList()

  var pendingBlank: Position? by mutableStateOf(null)
    private set
  private var blankFromArrow = false

  val drag = TileDrag()

  fun reset(board: Board, rack: List<String>) {
    state.reset(board)
    if (rack != tiles) replaceRack(rack)
    recall()
    exchanging = false
  }

  fun placement(): Placement? = state.placement()

  val hasPlaced: Boolean get() = placed.isNotEmpty()

  fun recall() {
    state.clear()
    placed.clear()
    selected = null
    toExchange.clear()
    exchanged = emptyList()
    state.arrow = null
    drag.end()
  }

  fun shuffle() {
    val fresh = order.shuffled()
    order.clear()
    order.addAll(fresh)
  }

  fun startExchange() {
    recall()
    exchanging = true
  }

  fun cancelExchange() {
    toExchange.clear()
    exchanging = false
  }

  fun tilesToExchange(): List<String> = toExchange.map { tiles[it] }

  fun confirmExchange(): List<String> {
    val chosen = tilesToExchange()
    exchanged = toExchange.toList()
    cancelExchange()
    return chosen
  }

  fun tapRack(i: Int) {
    when {
      i in placed.values -> Unit
      exchanging -> toggleExchange(i)
      state.arrow != null && selected == null -> placeAtArrow(i)
      else -> selectOrSwap(i)
    }
  }

  fun tapBoard(pos: Position) {
    val current = selected
    when {
      pos in state.pending -> takeBack(pos)
      state.board[pos] != null -> Unit
      current != null -> placeSelected(current, pos)
      else -> state.toggleArrow(pos)
    }
  }

  fun drop() {
    val i = drag.tile ?: return
    val from = drag.from
    val square = drag.square()
    val slot = drag.slot(order)
    drag.end()
    when {
      square != null -> if (from == null) dropOnBoard(i, square) else moveOnBoard(from, square)
      slot != null -> dropOnRack(i, slot)
      from != null -> takeBack(from)
    }
  }

  fun dropOnBoard(i: Int, pos: Position) {
    if (i !in placed.values && isFree(pos)) placeSelected(i, pos)
  }

  fun moveOnBoard(from: Position, to: Position) {
    if (!isFree(to)) return
    val i = placed.remove(from) ?: return
    state.remove(from)?.let { state.place(to, it) }
    placed[to] = i
  }

  fun dropOnRack(i: Int, slot: Int) {
    placed.entries.firstOrNull { it.value == i }?.let { takeBack(it.key) }
    order.remove(i)
    order.add(slot.coerceIn(0, order.size), i)
    selected = null
  }

  fun placeBlank(letter: String?) {
    val pos = pendingBlank
    val i = selected
    if (letter != null && pos != null && i != null) {
      place(pos, Tile(letter, blank = true), i)
      if (blankFromArrow) state.advanceArrow()
    } else {
      selected = null
    }
    pendingBlank = null
  }

  private fun replaceRack(rack: List<String>) {
    if (tiles.isNotEmpty()) staying = stayingTiles()
    tiles = rack
    order.clear()
    order.addAll(arrangeRack(staying, rack))
  }

  private fun stayingTiles(): List<String> = when (renewal) {
    RackRenewal.MY_PLAYS -> order.filter { it !in placed.values && it !in exchanged }.map { tiles[it] }
    RackRenewal.MASTER_PLAYS -> order.map { tiles[it] }
    RackRenewal.WHOLE -> emptyList()
  }

  private fun toggleExchange(i: Int) {
    if (i in toExchange) toExchange.remove(i) else toExchange.add(i)
  }

  private fun placeAtArrow(i: Int) {
    val at = state.arrow?.position ?: return
    if (isBlank(i)) return askBlankLetter(i, at, fromArrow = true)
    place(at, Tile(tiles[i]), i)
    state.advanceArrow()
  }

  private fun selectOrSwap(i: Int) {
    selected = when (val current = selected) {
      null -> i
      i -> null
      else -> null.also { swap(current, i) }
    }
  }

  private fun swap(a: Int, b: Int) {
    val ia = order.indexOf(a)
    val ib = order.indexOf(b)
    order[ia] = b
    order[ib] = a
  }

  private fun takeBack(pos: Position) {
    state.remove(pos)
    placed.remove(pos)
  }

  private fun placeSelected(i: Int, pos: Position) {
    if (isBlank(i)) askBlankLetter(i, pos, fromArrow = false) else place(pos, Tile(tiles[i]), i)
  }

  private fun askBlankLetter(i: Int, pos: Position, fromArrow: Boolean) {
    selected = i
    blankFromArrow = fromArrow
    pendingBlank = pos
  }

  private fun isFree(pos: Position): Boolean = state.board[pos] == null && pos !in state.pending

  private fun isBlank(i: Int): Boolean = tiles[i] == Letters.BLANK

  private fun place(pos: Position, tile: Tile, i: Int) {
    state.place(pos, tile)
    placed[pos] = i
    selected = null
  }
}
