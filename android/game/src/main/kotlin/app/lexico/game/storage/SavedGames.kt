package app.lexico.game.storage

import java.io.File

/** Las modalidades que se pueden guardar a medias. ENDGAME (Finales) se juega como una clasica. */
enum class Mode { CLASSIC, DUPLICATE, ENDGAME }

/** Una partida en curso guardada: su identificador, su modalidad y cuando se guardo. */
data class SavedGame(val id: String, val mode: Mode, val updatedAt: Long)

/**
 * Las partidas en curso, una por archivo (`<id>.<modalidad>`) con el texto que da el motor al
 * guardarlas. Al terminar o abandonar una partida, su archivo se borra; los registros de las
 * terminadas los escribe el motor en otra carpeta.
 */
class SavedGames(private val dir: File) {
  /** Las partidas en curso, la mas reciente primero. */
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
