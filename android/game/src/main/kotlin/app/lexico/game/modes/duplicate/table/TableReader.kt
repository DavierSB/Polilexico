package app.lexico.game.modes.duplicate.table

import app.lexico.game.engine.moveText
import app.lexico.game.engine.parseBoard
import app.lexico.game.engine.placedSquares
import app.lexico.game.engine.rackTiles
import app.lexico.model.Board
import org.json.JSONArray
import org.json.JSONObject

enum class TablePhase { PLAYING, RESULTS, FINISHED }

data class TableEntry(val name: String, val text: String, val points: Int, val total: Int)

data class TableRound(val number: Int, val entries: List<TableEntry>)

data class TableState(
  val board: Board,
  val rack: List<String>,
  val phase: TablePhase,
  val round: Int,
  val remainingMs: Long?,
  val proposal: TableEntry?,
  val ready: Int,
  val waiting: Int,
  val iAmReady: Boolean,
  val live: Boolean,
  val players: List<String>,
  val rounds: List<TableRound>,
  val bag: Int,
)

const val MASTER = "Máster"

fun readTable(json: String): TableState {
  val o = JSONObject(json)
  val rounds = o.optJSONArray("Rounds") ?: JSONArray()
  val board = parseBoard(o.getString("Board"), latestSquares(rounds))
  return TableState(
    board = board, rack = rackTiles(o.getString("Rack")), phase = phase(o.getString("Phase")), round = o.getInt("Round"),
    remainingMs = o.getLong("RemainingMs").takeIf { it >= 0 }, proposal = proposal(board, o.optJSONObject("Proposal")),
    ready = o.getInt("Ready"), waiting = o.getInt("Waiting"), iAmReady = o.getBoolean("IAmReady"), live = o.getBoolean("Live"),
    players = strings(o.optJSONArray("Players")), rounds = objects(rounds).map { round(board, it) },
    bag = rackTiles(o.getString("Unseen")).size,
  )
}

private fun phase(text: String): TablePhase = when (text) {
  "playing" -> TablePhase.PLAYING
  "results" -> TablePhase.RESULTS
  else -> TablePhase.FINISHED
}

private fun round(board: Board, o: JSONObject): TableRound =
  TableRound(o.getInt("Number"), objects(o.optJSONArray("Plays") ?: JSONArray()).map { entry(board, it) })

private fun entry(board: Board, o: JSONObject): TableEntry = TableEntry(
  o.getString("Name"), playText(board, o.getString("Kind"), o.getString("Coords"), o.getString("Tiles")), o.getInt("Points"), o.getInt("Total"),
)

private fun proposal(board: Board, o: JSONObject?): TableEntry? = o?.let {
  TableEntry("", playText(board, it.getString("Kind"), it.getString("Coords"), it.getString("Tiles")), it.getInt("Score"), 0)
}

private fun playText(board: Board, kind: String, coords: String, tiles: String): String = when (kind) {
  "play" -> moveText(board, "$coords $tiles")
  "invalid" -> "inválida: " + moveText(board, "$coords $tiles")
  "pass" -> "pase"
  else -> "sin jugada"
}

private fun latestSquares(rounds: JSONArray) =
  objects(rounds).lastOrNull()?.optJSONArray("Plays")?.let(::objects)?.firstOrNull()
    ?.let { placedSquares(it.getString("Coords"), it.getString("Tiles")) }.orEmpty()

private fun objects(a: JSONArray): List<JSONObject> = (0 until a.length()).map(a::getJSONObject)

private fun strings(a: JSONArray?): List<String> = a?.let { (0 until it.length()).map(it::getString) }.orEmpty()
