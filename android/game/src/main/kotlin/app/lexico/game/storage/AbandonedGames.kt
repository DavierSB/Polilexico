package app.lexico.game.storage

import java.io.File

data class UnfinishedGame(val mode: Mode, val opponent: String, val at: Long)

class AbandonedGames(private val file: File) {
  fun list(): List<UnfinishedGame> = runCatching { file.readLines().mapNotNull(::parse) }.getOrDefault(emptyList())

  fun add(game: UnfinishedGame) {
    file.parentFile?.mkdirs()
    file.appendText("${game.at}\t${game.mode.name}\t${game.opponent}\n")
  }

  private fun parse(line: String): UnfinishedGame? {
    val parts = line.split('\t')
    if (parts.size != 3) return null
    val mode = Mode.entries.find { it.name == parts[1] } ?: return null
    return parts[0].toLongOrNull()?.let { UnfinishedGame(mode, parts[2], it) }
  }
}
