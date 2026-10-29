package app.lexico.model.dictionary

internal class Relation(private val via: String, private val parts: List<String>, private val category: String) {
  fun describe(lemma: String, middle: String?): String {
    if (via == DIRECT_VIA || parts.isEmpty()) return ""
    val targets = parts.indices.map { if (it == 0 && middle != null && parts.size > 1) middle else lemma }
    return parts.zip(targets).joinToString(", ") { (part, target) -> phrase(part, target) }.replaceFirstChar(Char::uppercaseChar)
  }

  fun ordered(senses: List<Sense>): List<Sense> = senses.sortedByDescending(::fits)

  private fun fits(sense: Sense): Boolean = when (category) {
    VERB -> sense.category == Category.VERB
    NOMINAL -> sense.category == Category.NOUN || sense.category == Category.ADJECTIVE
    else -> false
  }

  private fun phrase(part: String, target: String): String = if (part.endsWith('-')) "$part + $target" else "$part de $target"

  companion object {
    private const val DIRECT_VIA = "directa"
    private const val VERB = "verbo"
    private const val NOMINAL = "nominal"

    val DIRECT = Relation(DIRECT_VIA, emptyList(), "")

    fun parse(line: String): Relation {
      val fields = line.split('\t')
      val parts = fields.getOrElse(1) { "" }.split("; ").filter { it.isNotEmpty() }
      return Relation(fields[0], parts, fields.getOrElse(2) { "" })
    }
  }
}
