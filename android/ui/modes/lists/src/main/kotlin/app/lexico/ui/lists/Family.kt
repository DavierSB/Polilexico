package app.lexico.ui.lists

const val MIN_TILES = 2
const val MAX_TILES = 10

enum class Kind(val label: String) { ALL("Todas"), VERBAL("Verbales"), NON_VERBAL("No verbales") }

enum class Order(val label: String) { POINTS("Importancia"), ALPHABETIC("A–Z"), LENGTH("Longitud") }

class Family(
  val verbal: Boolean,
  val nonVerbal: Boolean,
  val points: Long,
  val permille: Int,
  val code: String,
  val words: List<String>,
  val isWord: Boolean = false,
  val owners: IntArray = IntArray(0),
  val index: Int = -1,
) {
  val head: String get() = words.first()

  val key: String get() = (if (isWord) "w:" else if (verbal) "v:" else "n:") + head

  fun traits(word: String): List<VerbTrait> {
    if (!verbal || isWord) return emptyList()
    val codes = code.split(' ').firstOrNull { it.startsWith("$word:") }?.substringAfter(':').orEmpty()
    return VerbTrait.entries.filter { it.code in codes }
  }
}

enum class VerbTrait(val code: Char, val label: String) {
  INTRANSITIVE('i', "Intransitivo"),
  IMPERSONAL('m', "Impersonal"),
  UNIPERSONAL('u', "Unipersonal"),
  DEFECTIVE('d', "Defectivo"),
  PARTICIPLE('p', "Participio irregular"),
}

enum class Likeness(val code: Char) {
  SOUND('1'),
  AR_EAR('2'),
  FINAL_VOWEL('3'),
  CLUSTER('4'),
  VOWEL('5'),
  COMMON_CONSONANT('6'),
  RARE_CONSONANT('7'),
  CROSSED_CONSONANT('8'),
}

data class Similar(val word: String, val likeness: Likeness)
