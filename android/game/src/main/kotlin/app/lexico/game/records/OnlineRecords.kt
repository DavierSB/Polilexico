package app.lexico.game.records

import java.io.File

class OnlineRecords internal constructor(private val file: File) {
  @Synchronized
  fun add(path: String) {
    if (!contains(path)) file.appendText(File(path).name + "\n")
  }

  @Synchronized
  fun contains(path: String): Boolean = File(path).name in names()

  private fun names(): Set<String> = if (file.exists()) file.readLines().toSet() else emptySet()
}
