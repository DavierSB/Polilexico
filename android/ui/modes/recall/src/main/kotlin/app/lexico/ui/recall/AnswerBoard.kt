package app.lexico.ui.recall

/**
 * Armar una palabra tocando fichas, como en el quiz de FamiliasScrabble. `fixed` son las fichas
 * que ya estan en su casilla y no se mueven; `pool`, las del atril, barajadas y siempre en su
 * sitio; `slots[i]` es la ficha del pool puesta en la casilla `i`, o `null` si no hay ninguna.
 */
data class AnswerBoard(val fixed: Map<Int, String>, val pool: List<String>, val slots: List<Int?>) {
  /** Fichas del pool ya puestas en la palabra. */
  val used: Set<Int> get() = slots.filterNotNull().toSet()

  val isComplete: Boolean get() = slots.indices.none(::isEmpty)

  /** Lo armado, ficha a ficha (`null` en las casillas vacias). */
  val letters: List<String?> get() = slots.indices.map { fixed[it] ?: slots[it]?.let(pool::get) }

  fun isFixed(slot: Int): Boolean = slot in fixed

  /** Pone `pool[i]` en la primera casilla vacia. */
  fun place(i: Int): AnswerBoard {
    val free = slots.indices.firstOrNull(::isEmpty)
    if (free == null || i in used) return this
    return copy(slots = slots.toMutableList().also { it[free] = i })
  }

  /** Devuelve al pool la ficha de la casilla `slot` (las fijas no se mueven). */
  fun clear(slot: Int): AnswerBoard =
    if (slots.getOrNull(slot) == null) this else copy(slots = slots.toMutableList().also { it[slot] = null })

  /** Todas las fichas de vuelta al pool. */
  fun reset(): AnswerBoard = copy(slots = List(slots.size) { null })

  private fun isEmpty(slot: Int): Boolean = !isFixed(slot) && slots[slot] == null

  companion object {
    /** La palabra sin armar: sus fijas en su sitio y el resto en el atril. */
    fun start(word: WordToRecall): AnswerBoard =
      AnswerBoard(word.fixed.associateWith(word.tiles::get), word.scrambled, List(word.tiles.size) { null })
  }
}
