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

/**
 * Colocar fichas del atril en el tablero a mano, al estilo de ISC. Lo usan la clasica y la
 * duplicada; todo es local a la pantalla hasta que se pide la [placement].
 *
 * - Tocar una casilla vacia pone la flecha (horizontal); tocarla otra vez la pone vertical; una
 *   tercera vez la quita. Con flecha, cada ficha del atril que tocas se coloca en la flecha y la
 *   flecha avanza (saltando las casillas ocupadas).
 * - Sin flecha, tocar una ficha del atril la elige; tocar otra las intercambia de sitio
 *   (anagramar); con una elegida, tocar una casilla vacia la pone alli.
 * - Tocar una ficha tuya del tablero la devuelve al atril.
 * - En modo cambio, tocar fichas del atril las marca para cambiar.
 *
 * Tambien se puede arrastrar ([drag]): del atril a una casilla vacia, de una casilla a otra, del
 * tablero al atril (o fuera del tablero) para devolverla, y dentro del atril para reordenarlo.
 *
 * Al llegar un atril nuevo, las fichas que se quedan ([renewal]) conservan su orden y las
 * robadas van detras, barajadas.
 */
@Stable
class TilePlacer(private val renewal: RackRenewal = RackRenewal.MY_PLAYS) {
  val state = BoardState()

  /** Las fichas del atril, como las da el juego ("A", "CH", "?"). */
  var tiles: List<String> by mutableStateOf(emptyList())
    private set

  /** Orden en pantalla (indices en [tiles]). */
  val order = mutableStateListOf<Int>()

  /** Ficha del atril elegida (indice en [tiles]). */
  var selected: Int? by mutableStateOf(null)
    private set

  /** Fichas del atril ya puestas en el tablero, por casilla. */
  val placed = mutableStateMapOf<Position, Int>()

  var exchanging by mutableStateOf(false)
    private set

  /** Fichas del atril marcadas para cambiar (indices en [tiles]). */
  val toExchange = mutableStateListOf<Int>()

  /** Las fichas del ultimo cambio pedido, que se iran con el atril nuevo. */
  private var exchanged: List<Int> = emptyList()

  /** Las fichas que se quedan del ultimo atril, en el orden de pantalla (sobrevive a un atril vacio). */
  private var staying: List<String> = emptyList()

  /** Casilla donde va un comodin cuya letra hay que preguntar; `null` = no hay pregunta. */
  var pendingBlank: Position? by mutableStateOf(null)
    private set
  private var blankFromArrow = false

  /** La ficha que se esta arrastrando, si hay alguna. */
  val drag = TileDrag()

  /** Nuevo turno: este tablero y este atril. Se recoge todo lo provisional. */
  fun reset(board: Board, rack: List<String>) {
    state.reset(board)
    if (rack != tiles) replaceRack(rack)
    recall()
    exchanging = false
  }

  /** La jugada que forman las fichas puestas, o `null` si no estan en una linea continua. */
  fun placement(): Placement? = state.placement()

  val hasPlaced: Boolean get() = placed.isNotEmpty()

  /** Devuelve al atril todo lo puesto y quita la flecha y las marcas. */
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

  /** Las fichas marcadas para cambiar, en el orden del atril. */
  fun tilesToExchange(): List<String> = toExchange.map { tiles[it] }

  /** Pide el cambio: devuelve las fichas marcadas y sale del modo cambio. */
  fun confirmExchange(): List<String> {
    val chosen = tilesToExchange()
    exchanged = toExchange.toList()
    cancelExchange()
    return chosen
  }

  /** Un toque en la ficha `i` del atril (indice en [tiles]); ver las reglas arriba. */
  fun tapRack(i: Int) {
    when {
      i in placed.values -> Unit
      exchanging -> toggleExchange(i)
      state.arrow != null && selected == null -> placeAtArrow(i)
      else -> selectOrSwap(i)
    }
  }

  /** Un toque en una casilla del tablero; ver las reglas arriba. */
  fun tapBoard(pos: Position) {
    val current = selected
    when {
      pos in state.pending -> takeBack(pos)
      state.board[pos] != null -> Unit
      current != null -> placeSelected(current, pos)
      else -> state.toggleArrow(pos)
    }
  }

  /**
   * Suelta la ficha arrastrada donde apunta: en una casilla, en un hueco del atril o, si no cae
   * en ninguno, de vuelta a donde estaba (una del tablero, al atril).
   */
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

  /** La ficha `i` del atril, soltada en una casilla: se pone si esta libre. */
  fun dropOnBoard(i: Int, pos: Position) {
    if (i !in placed.values && isFree(pos)) placeSelected(i, pos)
  }

  /** Una ficha puesta pasa a otra casilla libre (un comodin conserva su letra). */
  fun moveOnBoard(from: Position, to: Position) {
    if (!isFree(to)) return
    val i = placed.remove(from) ?: return
    state.remove(from)?.let { state.place(to, it) }
    placed[to] = i
  }

  /** La ficha `i` va al hueco `slot` del atril (fichas a su izquierda); si estaba puesta, vuelve. */
  fun dropOnRack(i: Int, slot: Int) {
    placed.entries.firstOrNull { it.value == i }?.let { takeBack(it.key) }
    order.remove(i)
    order.add(slot.coerceIn(0, order.size), i)
    selected = null
  }

  /** Respuesta a [pendingBlank]: la letra elegida, o `null` si se cancelo. */
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

  /** Un atril nuevo, ordenado con [arrangeRack]. */
  private fun replaceRack(rack: List<String>) {
    if (tiles.isNotEmpty()) staying = stayingTiles()
    tiles = rack
    order.clear()
    order.addAll(arrangeRack(staying, rack))
  }

  /** Las fichas de ahora que se quedaran en el atril, en el orden de pantalla. */
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

  /** Elige la ficha `i`; si ya estaba elegida, la suelta; si habia otra, las intercambia. */
  private fun selectOrSwap(i: Int) {
    selected = when (val current = selected) {
      null -> i
      i -> null
      else -> null.also { swap(current, i) }
    }
  }

  /** Anagramar el atril: dos fichas cambian de sitio. */
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

  /** El comodin necesita letra: se pregunta y se pone al contestar ([placeBlank]). */
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
