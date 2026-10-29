package app.lexico.ui.lists

data class FilterSpec(
  val minLength: Int = MIN_TILES,
  val maxLength: Int = MAX_TILES,
  val contains: String = "",
  val fixed: String = "",
  val fixedEnd: String = "",
  val kind: Kind = Kind.ALL,
  val order: Order = Order.POINTS,
  val vowels: Int? = null,
  val consonants: Int? = null,
  val simpleOnly: Boolean = false,
  val words: Boolean = false,
) {
  fun filter(): Filter = Filter(
    minLength, maxLength, WordKeys.parse(contains), fixed.slots(), fixedEnd.slots(), kind, vowels, consonants, simpleOnly,
  )

  val isEmpty: Boolean get() = filter() == Filter()

  val unit: String get() = if (words) "palabras" else "familias"

  fun cleared(): FilterSpec = FilterSpec(order = order, words = words)

  fun summary(): List<String> = FilterType.entries.mapNotNull { it.line(this) }

  fun encode(): String = listOf(
    minLength, maxLength, contains.clean(), fixed.clean(), fixedEnd.clean(), kind.name, order.name,
    vowels ?: "", consonants ?: "", if (simpleOnly) "s" else "", if (words) "p" else "f",
  ).joinToString(SEPARATOR)

  private fun String.slots(): Map<Int, Char> =
    withIndex().filter { it.value != FREE && it.index < maxLength }.associate { it.index to it.value }

  companion object {
    const val FREE = '.'
    private const val SEPARATOR = "|"
    private const val FIELDS = 11

    fun decode(text: String): FilterSpec? {
      val c = text.split(SEPARATOR).takeIf { it.size == FIELDS } ?: return null
      return runCatching { fromFields(c) }.getOrNull()
    }

    private fun fromFields(c: List<String>): FilterSpec = FilterSpec(
      c[0].toInt(), c[1].toInt(), c[2], c[3], c[4], Kind.valueOf(c[5]), Order.valueOf(c[6]),
      c[7].toIntOrNull(), c[8].toIntOrNull(), c[9] == "s", c[10] == "p",
    )

    private fun String.clean(): String = filter { it != '|' && it != '\t' && it != '\n' }
  }
}
