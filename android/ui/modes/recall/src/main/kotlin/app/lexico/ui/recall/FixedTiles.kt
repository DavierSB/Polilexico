package app.lexico.ui.recall

import kotlin.random.Random

/**
 * Las fichas de una palabra larga que ya estan en el tablero, como en una partida de verdad: el
 * jugador solo tiene [RACK_SIZE] en el atril, asi que en una palabra de mas fichas las demas
 * estan fijas en su sitio. Cada grupo de fijas pegadas tiene que ser una palabra valida; una fija
 * suelta siempre vale. Siempre hay una eleccion con todas sueltas: en 15 fichas, 8 fijas caben
 * alternadas.
 */
object FixedTiles {
  const val RACK_SIZE = 7

  /**
   * Las casillas fijas de `tiles`, al azar entre todas las validas. `isWord` recibe las fichas
   * juntas, con los digrafos entre corchetes ("[CH]E"). Ninguna si caben en el atril.
   */
  fun choose(tiles: List<String>, random: Random = Random.Default, isWord: (String) -> Boolean = { false }): Set<Int> {
    val count = tiles.size - RACK_SIZE
    if (count <= 0) return emptySet()
    val valid = memoized(isWord)
    return subsets(tiles.size, count).filter { fixed -> runsAreWords(tiles, fixed, valid) }.toList().random(random)
  }

  /** Los grupos de casillas consecutivas de `fixed`, en orden. */
  fun runs(fixed: Set<Int>): List<List<Int>> =
    fixed.sorted().fold(mutableListOf<MutableList<Int>>()) { runs, i ->
      runs.apply { if (lastOrNull()?.last() == i - 1) last() += i else add(mutableListOf(i)) }
    }

  private fun runsAreWords(tiles: List<String>, fixed: Set<Int>, isWord: (String) -> Boolean): Boolean =
    runs(fixed).all { run -> run.size == 1 || isWord(dictionaryText(run.map(tiles::get))) }

  /** Todos los subconjuntos de `size` casillas de entre `n` (n <= 15: a lo sumo 6435). */
  private fun subsets(n: Int, size: Int): Sequence<Set<Int>> =
    (0 until (1 shl n)).asSequence()
      .filter { Integer.bitCount(it) == size }
      .map { mask -> (0 until n).filter { mask and (1 shl it) != 0 }.toSet() }

  /** El motor tarda en cada consulta, y los mismos grupos se repiten entre subconjuntos. */
  private fun memoized(isWord: (String) -> Boolean): (String) -> Boolean {
    val known = HashMap<String, Boolean>()
    return { word -> known.getOrPut(word) { isWord(word) } }
  }
}
