package app.lexico.ui.lists

fun Lexicon.tag(word: String): String? {
  val unit = word(word) ?: return null
  return if (unit.verbal) verbTag(unit) else nonVerbTag(word)
}

private fun Lexicon.verbTag(unit: Family): String? =
  owners(unit).firstOrNull { it.verbal }?.traits(unit.head)?.firstNotNullOfOrNull { it.tag }

private fun nonVerbTag(word: String): String? = NON_VERB.takeIf { VERB_ENDINGS.any(word::endsWith) }

private val VERB_ENDINGS = listOf("ar", "er", "ir")
private const val NON_VERB = "NoVerbo"
