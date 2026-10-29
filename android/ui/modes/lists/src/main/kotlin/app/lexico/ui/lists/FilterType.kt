package app.lexico.ui.lists

enum class FilterType(val title: String, val info: String) {
  KIND("Tipo de palabra", "Elige si la lista tendrá solo verbos o solo palabras que no son verbos (sustantivos, adjetivos y demás)."),
  LENGTH("Longitud", "Longitud de la palabra más pequeña de la familia, de un mínimo a un máximo. En los verbos se cuenta el infinitivo menos una letra: «cazar» cuenta como 4."),
  COUNTS("Vocales y consonantes", "Cuántas vocales y cuántas consonantes exactas debe tener la palabra. Puedes fijar solo una de las dos."),
  CONTAINS("Letras que debe contener", "Letras que la palabra tiene que llevar, en cualquier orden. Si escribes una letra dos veces, tiene que aparecer dos veces. Puedes pedir además que todas las demás letras sean simples, es decir, alguna de estas: A, E, I, O, U, C, D, L, N, R, S y T."),
  FIXED("Letras en posición", "Fija una letra en un lugar concreto contando desde el principio: por ejemplo, una C en la primera casilla."),
  FIXED_END("Letras en posición desde el final", "Fija una letra en un lugar concreto contando desde el final: por ejemplo, una S en la última casilla.");

  fun line(spec: FilterSpec): String? = when (this) {
    KIND -> kindLine(spec.kind)
    LENGTH -> lengthLine(spec.minLength, spec.maxLength)
    COUNTS -> countsLine(spec.vowels, spec.consonants)
    CONTAINS -> lettersLine(spec.filter().contains, spec.simpleOnly)
    FIXED -> positionsLine(spec.filter().fixed, "")
    FIXED_END -> positionsLine(spec.filter().fixedEnd, " desde el final")
  }

  fun isSet(spec: FilterSpec): Boolean = line(spec) != null

  fun started(spec: FilterSpec): FilterSpec = when (this) {
    KIND -> if (spec.kind == Kind.ALL) spec.copy(kind = Kind.VERBAL) else spec
    else -> spec
  }

  fun cleared(spec: FilterSpec): FilterSpec = when (this) {
    KIND -> spec.copy(kind = Kind.ALL)
    LENGTH -> spec.copy(minLength = MIN_TILES, maxLength = MAX_TILES)
    COUNTS -> spec.copy(vowels = null, consonants = null)
    CONTAINS -> spec.copy(contains = "", simpleOnly = false)
    FIXED -> spec.copy(fixed = "")
    FIXED_END -> spec.copy(fixedEnd = "")
  }
}

private fun kindLine(kind: Kind): String? = when (kind) {
  Kind.ALL -> null
  Kind.VERBAL -> "Verbos"
  Kind.NON_VERBAL -> "No verbos"
}

private fun lengthLine(min: Int, max: Int): String? = when {
  min == max -> "Exactamente ${count(min, "letra", "letras")}"
  min > MIN_TILES && max < MAX_TILES -> "De $min a $max letras"
  min > MIN_TILES -> "Al menos ${count(min, "letra", "letras")}"
  max < MAX_TILES -> "A lo más ${count(max, "letra", "letras")}"
  else -> null
}

private fun countsLine(vowels: Int?, consonants: Int?): String? {
  val parts = listOfNotNull(vowels?.let { count(it, "vocal", "vocales") }, consonants?.let { count(it, "consonante", "consonantes") })
  return parts.takeIf { it.isNotEmpty() }?.let { "Exactamente " + it.joinToString(" y ") }
}

private fun lettersLine(contains: String, simpleOnly: Boolean): String? {
  val letters = containsLine(contains) ?: return "Solo letras simples".takeIf { simpleOnly }
  return if (simpleOnly) "$letters; las demás, simples" else letters
}

private fun containsLine(contains: String): String? {
  val tiles = contains.map(WordKeys::label).ifEmpty { return null }
  if (tiles.size > 1 && tiles.toSet().size == 1) return "Con la letra ${tiles[0]} " + if (tiles.size == 2) "duplicada" else "${tiles.size} veces"
  return if (tiles.size == 1) "Con la letra ${tiles[0]}" else "Con las letras " + tiles.dropLast(1).joinToString(", ") + " y " + tiles.last()
}

private fun positionsLine(fixed: Map<Int, Char>, suffix: String): String? {
  if (fixed.isEmpty()) return null
  return fixed.toSortedMap().map { (i, c) -> "${WordKeys.label(c)} en la ${i + 1}ª" }.joinToString(", ") + " posición$suffix"
}

private fun count(n: Int, one: String, many: String): String = if (n == 1) "1 $one" else "$n $many"
