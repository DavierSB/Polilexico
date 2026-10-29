package app.lexico.model.dictionary

internal class SenseLine(val lemma: String, val senses: List<Sense>) {
  companion object {
    private const val FIELDS = 4

    fun parse(line: String): SenseLine {
      val fields = line.split('\t')
      val groups = fields.drop(1).chunked(FIELDS).filter { it.size == FIELDS }
      return SenseLine(groups.firstOrNull()?.get(0)?.ifEmpty { null } ?: fields[0], groups.map(::sense))
    }

    private fun sense(group: List<String>): Sense {
      val source = Source.entries.find { it.code == group[1].getOrNull(0) } ?: Source.WIKTIONARY
      val category = Category.entries.find { it.code == group[1].getOrNull(1) } ?: Category.OTHER
      return Sense(group[3], group[2], source, category)
    }
  }
}
