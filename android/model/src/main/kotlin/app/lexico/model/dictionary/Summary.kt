package app.lexico.model.dictionary

object Summary {
  private const val LONG = 60
  private const val SHORTEST_CLAUSE = 12
  private val LEAD = Regex("^(?:(?:[Ee]n|[Pp]or) [^,:]{1,40}[,:] *|[Dd]icho de [^,:]{1,60}[,:] *|[Cc]oloquial[,:]? *)+")
  private val SENTENCE_END = Regex("(?<=[a-záéíóúüñ)])[.;](?: |$)")
  private val TRAILING_NOTE = Regex("\\s*\\([^)]*\\)$")

  fun of(meanings: List<Meaning>): Sense? = meanings.firstNotNullOfOrNull { it.senses.firstOrNull() }?.let { it.copy(text = line(it.text)) }

  fun line(text: String): String {
    val sentence = SENTENCE_END.split(LEAD.replace(text.trim(), ""), 2).first()
    val clause = TRAILING_NOTE.replace(shortened(sentence), "").trim().trimEnd('.', ';', ',')
    return clause.replaceFirstChar(Char::uppercaseChar)
  }

  private fun shortened(sentence: String): String {
    val comma = sentence.indexOf(", ")
    return if (sentence.length > LONG && comma >= SHORTEST_CLAUSE) sentence.substring(0, comma) else sentence
  }
}
