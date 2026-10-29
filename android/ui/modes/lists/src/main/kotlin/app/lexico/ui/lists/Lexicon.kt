package app.lexico.ui.lists

import app.lexico.model.dictionary.Meaning
import java.util.concurrent.ConcurrentHashMap

class Lexicon internal constructor(
  val families: List<Family>,
  loadWords: () -> WordTable,
  loadSimilar: () -> List<String> = { emptyList() },
  private val lookUp: (String) -> List<Meaning> = { emptyList() },
) {
  private val table: WordTable by lazy(loadWords)
  val words: List<Family> get() = table
  private val similarLines: List<String> by lazy(loadSimilar)
  private val forms: List<IntArray> by lazy { table.wordsByFamily(families.size) }
  private val byKey: Map<String, Family> by lazy { families.associateBy { it.key } }
  private val members: MemberIndex by lazy { MemberIndex(families) }
  private val sorted = ConcurrentHashMap<Pair<Boolean, Order>, List<Family>>()

  fun units(words: Boolean): List<Family> = if (words) this.words else families

  fun totalPoints(words: Boolean): Long = units(words).sumOf { it.points }

  fun filter(spec: FilterSpec): List<Family> {
    val filter = spec.filter()
    return ordered(spec.words, spec.order).filter(filter::matches)
  }

  fun find(key: String): Family? = if (key.startsWith(WORD)) word(key.removePrefix(WORD)) else byKey[key]

  fun preload() {
    table
    forms
  }

  fun preloadSimilar() {
    similarLines
    members
  }

  fun word(key: String): Family? = table.find(key)

  fun familyOf(member: String): Family? = members.find(member)

  fun resolve(entry: String): Family? = find(entry) ?: familyOf(WordKeys.bare(entry))

  fun owners(word: Family): List<Family> = word.owners.map(families::get)

  fun forms(unit: Family): List<Family> = if (unit.isWord) listOf(unit) else forms.getOrNull(unit.index)?.map(table::get).orEmpty()

  fun similar(unit: Family): List<Similar> {
    val index = if (unit.isWord) unit.owners.firstOrNull() ?: return emptyList() else unit.index
    return similarLines.getOrNull(index).orEmpty().split(' ').mapNotNull(::similarOf)
  }

  fun meanings(wordKey: String): List<Meaning> = lookUp(wordKey)

  fun unitsOf(wordKey: String): List<Family> {
    val word = word(wordKey) ?: return emptyList()
    return listOf(word) + owners(word)
  }

  private fun similarOf(entry: String): Similar? {
    val likeness = Likeness.entries.find { it.code == entry.firstOrNull() } ?: return null
    return Similar(entry.drop(1), likeness)
  }

  private fun ordered(words: Boolean, order: Order): List<Family> = sorted.getOrPut(words to order) { sort(units(words), order) }

  private fun sort(units: List<Family>, order: Order): List<Family> = when (order) {
    Order.POINTS -> units
    Order.ALPHABETIC -> sortedView(units) { WordKeys.alphabetic(it.head) }
    Order.LENGTH -> sortedView(units) { it.head.length }
  }

  private fun <K : Comparable<K>> sortedView(units: List<Family>, key: (Family) -> K): List<Family> {
    val keys = units.map(key)
    val order = units.indices.sortedBy { keys[it] }.toIntArray()
    return object : AbstractList<Family>() {
      override val size: Int get() = order.size

      override fun get(index: Int): Family = units[order[index]]
    }
  }

  companion object {
    private const val WORD = "w:"

    fun parse(
      familyLines: List<String>,
      wordLines: () -> List<String>,
      similarLines: () -> List<String> = { emptyList() },
      lookUp: (String) -> List<Meaning> = { emptyList() },
    ): Lexicon = Lexicon(familyLines.mapIndexedNotNull(::family), { WordTable.parse(wordLines()) }, similarLines, lookUp)

    private fun family(index: Int, line: String): Family? {
      val c = line.split('\t').takeIf { it.size == 5 } ?: return null
      val verbal = c[0] == "v"
      return Family(verbal, !verbal, c[1].toLong(), c[2].toInt(), c[3], c[4].split(' '), index = index)
    }
  }
}
