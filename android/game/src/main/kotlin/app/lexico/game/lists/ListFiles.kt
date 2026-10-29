package app.lexico.game.lists

import java.io.File

class ListFiles(private val dir: File) {
  fun all(): Map<String, String> =
    dir.listFiles { file -> file.extension == EXTENSION }.orEmpty().associate { it.nameWithoutExtension to it.readText() }

  fun write(id: String, text: String) {
    dir.mkdirs()
    val temporary = File(dir, "$id.tmp").apply { writeText(text) }
    temporary.renameTo(File(dir, "$id.$EXTENSION"))
  }

  fun modified(id: String): Long = File(dir, "$id.$EXTENSION").lastModified()

  fun delete(id: String) {
    File(dir, "$id.$EXTENSION").delete()
  }

  private companion object {
    const val EXTENSION = "tsv"
  }
}
