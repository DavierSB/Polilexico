package app.lexico.ui.lists

import java.util.UUID

data class Folder(val path: List<String>, val families: Int, val learned: Int) {
  val name: String get() = path.last()

  val percent: Int get() = if (families == 0) 0 else learned * 100 / families

  companion object {
    fun of(path: List<String>, lists: List<WordList>): Folder {
      val members = lists.flatMap { it.members }.toSet()
      return Folder(path, members.size, lists.flatMap { it.learned }.toSet().count { it in members })
    }
  }
}

sealed interface CatalogEntry {
  data class Sub(val folder: Folder) : CatalogEntry

  data class Leaf(val list: WordList) : CatalogEntry
}

class ListCatalog(private val rows: List<Pair<List<String>, WordList?>>) {
  val lists: List<WordList> = rows.mapNotNull { it.second }

  fun entries(path: List<String>): List<CatalogEntry> {
    val added = mutableSetOf<List<String>>()
    return rows.mapNotNull { (folder, list) ->
      when {
        folder == path -> list?.let { CatalogEntry.Leaf(it) }
        folder.size > path.size && folder.take(path.size) == path -> folder.take(path.size + 1).takeIf(added::add)?.let { CatalogEntry.Sub(folderOf(it)) }
        else -> null
      }
    }
  }

  fun find(id: String): WordList? = lists.find { it.id == id }

  private fun folderOf(path: List<String>): Folder = Folder.of(path, lists.filter { it.folder.take(path.size) == path })

  companion object {
    const val MINE = "Mis listas"
    private const val PREFIX = "d-"

    fun isDefault(id: String): Boolean = id.startsWith(PREFIX)

    fun parse(lines: List<String>, saved: List<WordList>): ListCatalog {
      val progress = saved.associateBy { it.id }
      return ListCatalog(lines.map { it.split('\t') }.filter { it.size == 5 }.map { row -> row(row, progress) })
    }

    private fun row(row: List<String>, progress: Map<String, WordList>): Pair<List<String>, WordList?> {
      val folder = row[0].split('/')
      val list = if (row[1].isEmpty()) null else template(row, folder)?.let { it.withProgress(progress[it.id]) }
      return folder to list
    }

    private fun template(row: List<String>, folder: List<String>): WordList? {
      val spec = FilterSpec.decode(row[2]) ?: return null
      return WordList(idOf(row[0], row[1]), row[1], 0L, spec, words(row[4]), folder = folder, notes = words(row[3], '|'))
    }

    private fun WordList.withProgress(saved: WordList?): WordList =
      if (saved == null) this else copy(learned = saved.learned.filter { it in members }.toSet(), studied = saved.studied, completions = saved.completions, used = saved.used)

    private fun idOf(folder: String, name: String): String = PREFIX + UUID.nameUUIDFromBytes("$folder/$name".toByteArray())

    private fun words(text: String, separator: Char = ' '): List<String> = text.split(separator).filter { it.isNotEmpty() }
  }
}
