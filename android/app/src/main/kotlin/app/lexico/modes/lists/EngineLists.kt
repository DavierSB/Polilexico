package app.lexico.modes.lists

import app.lexico.game.Lexico
import app.lexico.game.lists.ListFiles
import app.lexico.ui.lists.HandSource
import app.lexico.ui.lists.Lexicon
import app.lexico.ui.lists.ListCatalog
import app.lexico.ui.lists.ListBook
import app.lexico.ui.lists.WordList
import java.util.concurrent.Executors
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield

object WordLexicon {
  private val background = Executors.newSingleThreadExecutor { Thread(it, "lexicon").apply { priority = Thread.MIN_PRIORITY; isDaemon = true } }
    .asCoroutineDispatcher()
  @Volatile private var source: Lexico? = null
  @Volatile private var warmed: Lexicon? = null
  private val lexicon: Lexicon by lazy { read(source!!) }

  val ready: Lexicon? get() = warmed

  suspend fun warmInBackground(lexico: Lexico) {
    delay(START_DELAY_MS)
    warm(lexico, background)
  }

  suspend fun warm(lexico: Lexico, dispatcher: CoroutineDispatcher = Dispatchers.Default): Lexicon {
    warmed?.let { return it }
    source = lexico
    return withContext(dispatcher) { steps(lexico).forEach { it(); yield() }; lexicon }.also { warmed = it }
  }

  private fun steps(lexico: Lexico): List<() -> Unit> =
    listOf({ lexicon }, { lexicon.preload() }, { lexicon.preloadSimilar() }, { lexico.dictionary.open() })

  private fun read(lexico: Lexico): Lexicon =
    Lexicon.parse(lexico.wordData.families(), lexico.wordData::words, lexico.wordData::similar, lexico.dictionary::meanings)

  private const val START_DELAY_MS = 3000L
}

fun listCatalog(lexico: Lexico, book: ListBook): ListCatalog =
  ListCatalog.parse(lexico.wordData.defaultLists(), book.all().filter { it.isDefault })

fun listBook(files: ListFiles): ListBook = object : ListBook {
  override fun all(): List<WordList> = files.all().mapNotNull { (id, text) -> WordList.decode(id, text)?.let { usedOf(it, files) } }

  override fun save(list: WordList) = files.write(list.id, list.copy(used = System.currentTimeMillis()).encode())

  override fun delete(id: String) = files.delete(id)
}

private fun usedOf(list: WordList, files: ListFiles): WordList = if (list.used > 0) list else list.copy(used = files.modified(list.id))

fun engineHands(lexico: Lexico): HandSource = object : HandSource {
  override suspend fun anagrams(tiles: List<String>): List<List<String>> = lexico.anagrammer.anagramTiles(tiles)

  override fun isWord(word: String): Boolean = lexico.anagrammer.isWord(word)
}
