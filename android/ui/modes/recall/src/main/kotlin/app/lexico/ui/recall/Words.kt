package app.lexico.ui.recall

import app.lexico.model.Board
import app.lexico.model.Placement
import kotlin.random.Random

/** Una colocacion de la partida a recordar ("H8 CA.A") con los puntos que hizo. */
data class ScoredPlacement(val placement: String, val score: Int)

/**
 * Una palabra a recordar: sus fichas en orden ("CH", "A", "RR", "O") y los puntos de la jugada.
 * `fixed` son las casillas que ya estan en el tablero (ver [FixedTiles]) y `scrambled`, las
 * demas fichas barajadas: el atril que se le presenta al jugador.
 */
data class WordToRecall(val tiles: List<String>, val score: Int, val fixed: Set<Int>, val scrambled: List<String>) {
  val text: String get() = tiles.joinToString("")
}

/** Las fichas como las lee el diccionario: los digrafos entre corchetes ("[CH]E"). */
internal fun dictionaryText(tiles: List<String>): String = tiles.joinToString("") { if (it.length > 1) "[$it]" else it }

/** De una partida, las palabras a recordar. */
object Words {
  /** Las palabras de menos fichas no cuentan. */
  const val MIN_TILES = 3

  /** Veces que se baraja una palabra buscando un orden distinto del original. */
  private const val SHUFFLE_TRIES = 20

  /**
   * La palabra principal de cada colocacion, en fichas y con sus puntos, en el orden de la
   * partida. Las letras que la jugada atraviesa salen del tablero; los comodines, como la letra
   * que representan. Lo que no se pueda colocar se salta.
   */
  fun mainWords(game: List<ScoredPlacement>): List<Pair<List<String>, Int>> {
    var board = Board.EMPTY
    return game.mapNotNull { (text, score) ->
      val placement = Placement.parseOrNull(text) ?: return@mapNotNull null
      board = runCatching { board.play(placement) }.getOrNull() ?: return@mapNotNull null
      placement.positions.map { board[it]!!.letter } to score
    }
  }

  /**
   * Las `count` palabras principales de mas puntos con [MIN_TILES] fichas o mas, sin repetir
   * (a igualdad de puntos, la que se jugo antes), en orden aleatorio, listas para armar. Si la
   * partida no da para tantas, las que haya. `isWord` decide que fichas fijas pueden ir pegadas.
   */
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

  /** La palabra con sus fijas elegidas y el resto de sus fichas barajadas en el atril. */
  private fun toRecall(tiles: List<String>, score: Int, random: Random, isWord: (String) -> Boolean): WordToRecall {
    val fixed = FixedTiles.choose(tiles, random, isWord)
    val rack = tiles.filterIndexed { i, _ -> i !in fixed }
    return WordToRecall(tiles, score, fixed, scramble(rack, random))
  }

  /** Las fichas barajadas; si se puede, en un orden distinto del de la palabra. */
  fun scramble(tiles: List<String>, random: Random = Random.Default): List<String> {
    var out = tiles.shuffled(random)
    repeat(SHUFFLE_TRIES) {
      if (out != tiles) return out
      out = tiles.shuffled(random)
    }
    return out
  }
}
