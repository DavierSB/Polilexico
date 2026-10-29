package app.lexico.ui.lists

const val MAX_MEMBERS = 500

data class WordList(
  val id: String,
  val name: String,
  val created: Long,
  val spec: FilterSpec,
  val members: List<String>,
  val learned: Set<String> = emptySet(),
  val studied: Int = 0,
  val completions: Int = 0,
  val folder: List<String> = emptyList(),
  val notes: List<String> = emptyList(),
  val used: Long = 0L,
) {
  val isDefault: Boolean get() = ListCatalog.isDefault(id)

  val summary: List<String> get() = spec.summary() + notes

  private val memberSet: Set<String> by lazy { members.toSet() }

  val pending: List<String> get() = members.filter { it !in learned }

  val percent: Int get() = if (members.isEmpty()) 0 else learned.size * 100 / members.size

  val isComplete: Boolean get() = members.isNotEmpty() && learned.size >= members.size

  fun inProgress(now: Long): Boolean = !isComplete && learned.isNotEmpty() && !isStale(now)

  private fun isStale(now: Long): Boolean = learned.size * 100 < STALE_PERCENT * members.size && now - used > STALE_MS

  fun has(key: String): Boolean = key in memberSet

  fun learn(keys: Collection<String>): WordList {
    val next = copy(learned = learned + keys.filter(memberSet::contains))
    return if (next.isComplete && !isComplete) next.copy(completions = completions + 1) else next
  }

  fun restart(): WordList = copy(learned = emptySet())

  fun encode(): String = listOf(
    NAME to name.replace(Regex("[\\t\\n]"), " "),
    CREATED to created.toString(),
    SPEC to spec.encode(),
    MEMBERS to members.joinToString(" "),
    LEARNED to learned.joinToString(" "),
    STUDIED to studied.toString(),
    COMPLETIONS to completions.toString(),
    USED to used.toString(),
  ).joinToString("\n") { (field, value) -> "$field\t$value" }

  companion object {
    private const val NAME = "name"
    private const val CREATED = "created"
    private const val SPEC = "spec"
    private const val MEMBERS = "members"
    private const val LEARNED = "learned"
    private const val STUDIED = "studied"
    private const val COMPLETIONS = "completions"
    private const val USED = "used"
    private const val STALE_PERCENT = 5
    private const val STALE_MS = 3 * 24 * 60 * 60 * 1000L

    fun decode(id: String, text: String): WordList? {
      val fields = text.lines().map { it.split('\t', limit = 2) }.filter { it.size == 2 }.associate { it[0] to it[1] }
      return runCatching { fromFields(id, fields) }.getOrNull()
    }

    private fun fromFields(id: String, fields: Map<String, String>): WordList = WordList(
      id, fields.getValue(NAME), fields.getValue(CREATED).toLong(), FilterSpec.decode(fields.getValue(SPEC))!!,
      words(fields[MEMBERS]), words(fields[LEARNED]).toSet(), fields[STUDIED]?.toIntOrNull() ?: 0,
    ).let { it.copy(completions = fields[COMPLETIONS]?.toIntOrNull() ?: if (it.isComplete) 1 else 0, used = fields[USED]?.toLongOrNull() ?: 0L) }

    private fun words(text: String?): List<String> = text.orEmpty().split(' ').filter { it.isNotEmpty() }
  }
}

interface ListBook {
  fun all(): List<WordList>

  fun save(list: WordList)

  fun delete(id: String)
}
