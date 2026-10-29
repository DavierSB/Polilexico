package app.lexico.model.dictionary

internal class Entry(val lemma: String, val relation: Relation, val middle: String?) {
  companion object {
    fun parse(spelled: String, text: String, relations: List<Relation>): Entry {
      val fields = text.split(',')
      val middle = fields.getOrNull(2)?.let { Spelling.relative(spelled, it) }
      return Entry(Spelling.relative(spelled, fields[0]), relations.getOrElse(fields[1].toInt()) { Relation.DIRECT }, middle)
    }
  }
}

internal object Spelling {
  private val DIGRAPHS = mapOf('C' to "ch", 'L' to "ll", 'R' to "rr")

  fun plain(word: String): String = buildString { word.forEach { append(DIGRAPHS[it] ?: it) } }

  fun relative(base: String, code: String): String {
    val digits = code.takeWhile(Char::isDigit)
    return base.dropLast(digits.toInt()) + code.drop(digits.length)
  }
}
