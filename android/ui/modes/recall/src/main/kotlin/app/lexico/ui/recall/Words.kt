package app.lexico.ui.recall

import app.lexico.model.Board
import app.lexico.model.Placement
import kotlin.random.Random

data class ScoredPlacement(val placement: String, val score: Int)

data class WordToRecall(val tiles: List<String>, val score: Int, val fixed: Set<Int>, val scrambled: List<String>) {
  val text: String get() = tiles.joinToString("")
}

internal fun dictionaryText(tiles: List<String>): String = tiles.joinToString("") { if (it.length > 1) "[$it]" else it }

object Words {
  const val MIN_TILES = 3

  private const val SHUFFLE_TRIES = 20

  fun mainWords(game: List<ScoredPlacement>): List<Pair<List<String>, Int>> {
    var board = Board.EMPTY
    return game.mapNotNull { (text, score) ->
      val placement = Placement.parseOrNull(text) ?: return@mapNotNull null
      board = runCatching { board.play(placement) }.getOrNull() ?: return@mapNotNull null
      placement.positions.map { board[it]!!.letter } to score
    }
  }

  fun pick(
    game: List<ScoredPlacement>, count: Int, random: Random = Random.Default, isWord: (String) -> Boolean = { false },
  ): List<WordToRecall> =
    mainWords(game)
      .filter { (tiles, _) -> tiles.size >= MIN_TILES }
      .sortedByDescending { (_, score) -> score }
      .distinctBy { (tiles, _) -> tiles }
      .take(count)
      .shuffled(random)
      .map { (tiles, score) -> toRecall(tiles, score, random, isWord) }

  private fun toRecall(tiles: List<String>, score: Int, random: Random, isWord: (String) -> Boolean): WordToRecall {
    val fixed = FixedTiles.choose(tiles, random, isWord)
    val rack = tiles.filterIndexed { i, _ -> i !in fixed }
    return WordToRecall(tiles, score, fixed, scramble(rack, random))
  }

  fun scramble(tiles: List<String>, random: Random = Random.Default): List<String> {
    var out = tiles.shuffled(random)
    repeat(SHUFFLE_TRIES) {
      if (out != tiles) return out
      out = tiles.shuffled(random)
    }
    return out
  }
}
