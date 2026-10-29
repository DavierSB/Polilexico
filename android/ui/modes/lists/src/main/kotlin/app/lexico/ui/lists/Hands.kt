package app.lexico.ui.lists

import app.lexico.ui.recall.AnswerBoard
import app.lexico.ui.recall.FixedTiles
import app.lexico.ui.recall.Words
import kotlin.math.pow
import kotlin.random.Random

interface HandSource {
  suspend fun anagrams(tiles: List<String>): List<List<String>>

  fun isWord(word: String): Boolean
}

data class Hand(val unit: Family, val tiles: List<String>, val fixed: Set<Int>, val rack: List<String>, val answers: List<List<String>>) {
  fun board(): AnswerBoard = AnswerBoard(fixed.associateWith(tiles::get), rack, List(tiles.size) { null })
}

object Hands {
  suspend fun of(unit: Family, lexicon: Lexicon, filter: Filter, source: HandSource, random: Random = Random.Default): Hand? {
    val tiles = WordKeys.tiles(form(lexicon.forms(unit), filter, random) ?: return null)
    val all = source.anagrams(tiles).distinct()
    if (tiles !in all) return null
    val fixed = runCatching { FixedTiles.choose(tiles, random, source::isWord) }.getOrNull() ?: return null
    val answers = all.filter { answer -> fixed.all { answer[it] == tiles[it] } }
    return Hand(unit, tiles, fixed, Words.scramble(tiles.filterIndexed { i, _ -> i !in fixed }, random), answers)
  }

  fun form(forms: List<Family>, filter: Filter, random: Random = Random.Default): String? {
    val candidates = forms.filter { filter.matches(it.head) }.ifEmpty { return null }
    var ticket = random.nextDouble() * candidates.sumOf(::weight)
    return (candidates.firstOrNull { candidate -> ticket -= weight(candidate); ticket < 0 } ?: candidates.last()).head
  }

  private fun weight(form: Family): Double = 2.0.pow(maxOf(0, form.permille) / 100.0)
}
