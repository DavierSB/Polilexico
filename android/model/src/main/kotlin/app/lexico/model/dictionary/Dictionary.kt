package app.lexico.model.dictionary

import java.io.File

class Dictionary(directory: File) {
  private val words = SortedLines(File(directory, WORDS))
  private val senses = SortedLines(File(directory, SENSES))
  private val demonyms = SortedLines(File(directory, DEMONYMS))
  private val relations: List<Relation> = File(directory, RELATIONS).readLines().map(Relation::parse)

  fun meanings(word: String): List<Meaning> = Senses.distinct(listOfNotNull(demonym(word)) + entries(word).mapNotNull(::meaning))

  private fun demonym(word: String): Meaning? {
    val line = demonyms.find(word) ?: return null
    return Meaning(Spelling.plain(word), "", listOf(Sense(line.substringAfter('\t'), "", Source.DEMONYMS, Category.ADJECTIVE)))
  }

  private fun entries(word: String): List<Entry> {
    val spelled = Spelling.plain(word)
    val line = words.find(word) ?: return listOf(Entry(spelled, Relation.DIRECT, null))
    return line.substringAfter('\t').split(';').map { Entry.parse(spelled, it, relations) }
  }

  private fun meaning(entry: Entry): Meaning? {
    val line = senses.find(entry.lemma) ?: return null
    val parsed = SenseLine.parse(line)
    return Meaning(parsed.lemma, entry.relation.describe(parsed.lemma, entry.middle), entry.relation.ordered(parsed.senses))
  }

  companion object {
    const val WORDS = "words.txt"
    const val SENSES = "senses.txt"
    const val RELATIONS = "relations.txt"
    const val DEMONYMS = "demonyms.txt"
  }
}
