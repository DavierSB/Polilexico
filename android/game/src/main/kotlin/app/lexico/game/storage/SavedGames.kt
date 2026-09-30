package app.lexico.game.storage

import java.io.File

enum class Mode { CLASSIC, DUPLICATE, ENDGAME }

data class SavedGame(val id: String, val mode: Mode, val updatedAt: Long)

class SavedGames(private val dir: File) {
  fun list(): List<SavedGame> =
    dir.listFiles().orEmpty().mapNotNull(::savedGameOf).sortedByDescending { it.updatedAt }

  fun read(game: SavedGame): String = file(game.id, game.mode).readText()

  fun write(id: String, mode: Mode, text: String) {
    dir.mkdirs()
    val tmp = File(dir, "$id.tmp")
    tmp.writeText(text)
    tmp.renameTo(file(id, mode))
  }

  fun delete(id: String, mode: Mode) {
    file(id, mode).delete()
  }

  private fun file(id: String, mode: Mode): File = File(dir, "$id.${mode.name.lowercase()}")

  private fun savedGameOf(f: File): SavedGame? {
    val mode = Mode.entries.find { it.name.lowercase() == f.extension } ?: return null
    return SavedGame(f.nameWithoutExtension, mode, f.lastModified())
  }
}
