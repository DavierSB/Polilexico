package app.lexico.ui.board

import app.lexico.model.Board
import app.lexico.model.Position

fun wordsAt(board: Board, p: Position): List<String> =
  if (!filled(board, p)) emptyList() else DIRECTIONS.mapNotNull { (rows, columns) -> wordThrough(board, p, rows, columns) }

private val DIRECTIONS = listOf(0 to 1, 1 to 0)

private fun wordThrough(board: Board, p: Position, rows: Int, columns: Int): String? {
  var start = p
  while (filled(board, start.step(-rows, -columns))) start = start.step(-rows, -columns)
  val tiles = generateSequence(start) { it.step(rows, columns) }.takeWhile { filled(board, it) }.map { board[it]!!.letter }.toList()
  return tiles.takeIf { it.size >= 2 }?.joinToString("")
}

private fun filled(board: Board, p: Position): Boolean = p.onBoard && board[p] != null

private fun Position.step(rows: Int, columns: Int): Position = Position(row + rows, column + columns)
