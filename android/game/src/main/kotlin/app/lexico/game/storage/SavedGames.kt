package app.lexico.game.storage

import app.lexico.go.classic.Classic
import app.lexico.go.duplicate.Duplicate
import java.io.File

enum class Mode { CLASSIC, DUPLICATE, ENDGAME }

data class SavedGame(val id: String, val mode: Mode, val updatedAt: Long)

data class GameProgress(
  val myScore: Int, val opponentScore: Int, val opponent: String? = null, val round: Int? = null, val humanStarts: Boolean = true,
  val host: String? = null,
)

class SavedGames(private val dir: File, private val abandoned: AbandonedGames) {
  fun list(): List<SavedGame> =
    dir.listFiles().orEmpty().mapNotNull(::savedGameOf).sortedByDescending { it.updatedAt }

  fun unfinished(): List<UnfinishedGame> = list().map { UnfinishedGame(it.mode, progress(it)?.opponent.orEmpty(), it.updatedAt) }

  fun abandonedGames(): List<UnfinishedGame> = abandoned.list()

  fun read(game: SavedGame): String = file(game.id, game.mode).readText()

  fun progress(game: SavedGame): GameProgress? = runCatching {
    val text = read(game)
    when (game.mode) {
      Mode.DUPLICATE -> Duplicate.readProgress(text).let { GameProgress(it.humanTotal.toInt(), it.masterTotal.toInt(), round = it.round.toInt()) }
      Mode.CLASSIC, Mode.ENDGAME -> Classic.readProgress(text).let { GameProgress(it.humanTotal.toInt(), it.botTotal.toInt(), it.botName, humanStarts = it.humanStarts, host = it.humanName.takeIf { _ -> it.local }) }
    }
  }.getOrNull()

  fun write(id: String, mode: Mode, text: String) {
    dir.mkdirs()
    val tmp = File(dir, "$id.tmp")
    tmp.writeText(text)
    tmp.renameTo(file(id, mode))
  }

  fun delete(id: String, mode: Mode) {
    file(id, mode).delete()
  }

  fun abandon(id: String, mode: Mode) {
    val opponent = progress(SavedGame(id, mode, 0))?.opponent.orEmpty()
    abandoned.add(UnfinishedGame(mode, opponent, System.currentTimeMillis()))
    delete(id, mode)
  }

  private fun file(id: String, mode: Mode): File = File(dir, "$id.${mode.name.lowercase()}")

  private fun savedGameOf(f: File): SavedGame? {
    val mode = Mode.entries.find { it.name.lowercase() == f.extension } ?: return null
    return SavedGame(f.nameWithoutExtension, mode, f.lastModified())
  }
}
