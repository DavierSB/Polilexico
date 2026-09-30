package app.lexico.ui.recall

data class AnswerBoard(val fixed: Map<Int, String>, val pool: List<String>, val slots: List<Int?>) {
  val used: Set<Int> get() = slots.filterNotNull().toSet()

  val isComplete: Boolean get() = slots.indices.none(::isEmpty)

  val letters: List<String?> get() = slots.indices.map { fixed[it] ?: slots[it]?.let(pool::get) }

  fun isFixed(slot: Int): Boolean = slot in fixed

  fun place(i: Int): AnswerBoard {
    val free = slots.indices.firstOrNull(::isEmpty)
    if (free == null || i in used) return this
    return copy(slots = slots.toMutableList().also { it[free] = i })
  }

  fun clear(slot: Int): AnswerBoard =
    if (slots.getOrNull(slot) == null) this else copy(slots = slots.toMutableList().also { it[slot] = null })

  fun reset(): AnswerBoard = copy(slots = List(slots.size) { null })

  private fun isEmpty(slot: Int): Boolean = !isFixed(slot) && slots[slot] == null

  companion object {
    fun start(word: WordToRecall): AnswerBoard =
      AnswerBoard(word.fixed.associateWith(word.tiles::get), word.scrambled, List(word.tiles.size) { null })
  }
}
