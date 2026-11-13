package app.lexico.game.modes.classic

import app.lexico.model.Board
import app.lexico.model.Letters

internal fun unseenBy(board: Board, rack: List<String>): List<String> {
  val left = Letters.BAG.toMutableList()
  onBoard(board).forEach { left.remove(it) }
  rack.forEach { left.remove(it) }
  return left
}

private fun onBoard(board: Board): List<String> =
  (0 until Board.SIZE * Board.SIZE).mapNotNull { board[it / Board.SIZE, it % Board.SIZE] }
    .map { if (it.blank) Letters.BLANK else it.letter.uppercase() }
