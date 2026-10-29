package app.lexico.model.dictionary

import java.text.Normalizer

object Senses {
  const val SAME_SENSE = 0.8
  const val SAME_ENGLISH_SENSE = 0.6
  private const val SHORTEST_COMPARED = 3

  fun distinct(meanings: List<Meaning>): List<Meaning> {
    val kept = mutableListOf<Pair<Sense, Set<String>>>()
    return meanings.map { it.copy(senses = it.senses.filter { sense -> keep(sense, kept) }) }.filter { it.senses.isNotEmpty() }
  }

  fun overlap(first: Set<String>, second: Set<String>): Double {
    val smaller = minOf(first.size, second.size)
    return if (smaller < SHORTEST_COMPARED) 0.0 else (first intersect second).size.toDouble() / smaller
  }

  fun vocabulary(text: String): Set<String> {
    val bare = Normalizer.normalize(text.lowercase(), Normalizer.Form.NFD).replace(MARKS, "")
    return WORD.findAll(bare).map { it.value }.toSet() - FUNCTION_WORDS
  }

  private fun keep(sense: Sense, kept: MutableList<Pair<Sense, Set<String>>>): Boolean {
    val words = vocabulary(sense.text)
    if (kept.any { (other, seen) -> overlap(words, seen) >= threshold(sense, other) }) return false
    kept += sense to words
    return true
  }

  private fun threshold(first: Sense, second: Sense): Double =
    if (!first.source.spanish && !second.source.spanish) SAME_ENGLISH_SENSE else SAME_SENSE

  private val MARKS = Regex("[\u0300-\u036f]+")
  private val WORD = Regex("[a-zñ]+")
  private val FUNCTION_WORDS = (
    "de del la las el los lo un una unos unas y o u e a al en con por para que se su sus como es son le les mas muy " +
      "sin sobre the of or and to an in on for from with by as is be its it that which this"
    ).split(' ').toSet()
}
