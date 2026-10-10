package app.lexico.ui.games

import app.lexico.model.Board
import app.lexico.ui.common.RankedMove
import app.lexico.ui.common.Efficiency
import app.lexico.ui.common.Spread

data class ReviewView(
  val title: String,
  val turns: List<ReviewTurnView>,
  val startTurn: Int = 0,
  val opponent: String = "",
  val spread: Spread = Spread("", "", listOf(0)),
  val classic: Boolean = false,
  val efficiency: Efficiency? = null,
  val moves: MovesTable = MovesTable("", "", emptyList()),
)

data class ReviewTurnView(
  val number: Int,
  val player: String,
  val rack: List<String>,
  val board: Board,
  val candidates: List<RankedMove>,
  val marks: List<MarkView>,
  val myScore: Int = 0,
  val opponentScore: Int = 0,
  val mine: Boolean? = null,
  val unseen: List<String> = emptyList(),
  val bag: Int = 0,
)

data class MarkView(val who: String, val move: RankedMove, val rank: Int?)

data class PlayedMove(val text: String, val points: Int, val total: Int)

data class MovesRow(val turn: Int, val left: PlayedMove?, val right: PlayedMove?)

data class MovesTable(val left: String, val right: String, val rows: List<MovesRow>)
