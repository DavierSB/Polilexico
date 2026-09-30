package app.lexico.modes.recall

import app.lexico.game.Lexico
import app.lexico.game.modes.recall.RecallSetup
import app.lexico.game.records.RecallRecords
import app.lexico.ui.recall.GameSource
import app.lexico.ui.recall.RecallConfig
import app.lexico.ui.recall.Record
import app.lexico.ui.recall.RecordBook
import app.lexico.ui.recall.ScoredPlacement

/**
 * Las partidas de "¿Cuántas recuerdas?": HastyBot contra si mismo, del motor, que tambien dice
 * que fichas fijas pueden ir pegadas.
 */
fun engineGames(lexico: Lexico): GameSource = object : GameSource {
  override suspend fun game(): List<ScoredPlacement> =
    lexico.demoGames.next().map { ScoredPlacement(it.placement, it.score) }

  override fun isWord(word: String): Boolean = lexico.demoGames.isWord(word)
}

fun RecallConfig.toSetup(): RecallSetup = RecallSetup(intervalMs, wordsPerGame, lives, single)

/** El record de una serie con estas opciones, en los records del juego. */
fun recallRecords(records: RecallRecords, config: RecallConfig): RecordBook = object : RecordBook {
  private val setup = config.toSetup()

  override fun best(): Int = records.best(setup)

  override fun submit(recalled: Int): Record = records.submit(setup, recalled).let { Record(it.best, it.isNew) }
}
