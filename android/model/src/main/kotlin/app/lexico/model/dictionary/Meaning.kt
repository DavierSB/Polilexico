package app.lexico.model.dictionary

data class Meaning(val lemma: String, val relation: String, val senses: List<Sense>)

data class Sense(val text: String, val partOfSpeech: String, val source: Source, val category: Category)

enum class Source(val code: Char, val label: String, val spanish: Boolean) {
  WIKTIONARY('w', "Wikcionario", true),
  DRAE_1914('d', "DRAE 1914", true),
  ENGLISH_WIKTIONARY('e', "Wiktionary en inglés", false),
  DEMONYMS('g', "Estudio de gentilicios", true),
}

enum class Category(val code: Char) { NOUN('s'), ADJECTIVE('a'), VERB('v'), ADVERB('r'), OTHER('o') }
