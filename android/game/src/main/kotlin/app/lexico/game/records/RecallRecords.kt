package app.lexico.game.records

import android.content.Context
import app.lexico.game.modes.recall.RecallSetup

class RecallRecords(context: Context) {
  private val store = RecordStore(context, "records_recall")

  fun best(setup: RecallSetup): Int = store.best(key(setup))

  fun submit(setup: RecallSetup, recalled: Int): SeriesRecord = store.submit(key(setup), recalled)

  private fun key(setup: RecallSetup): String =
    "${setup.intervalMs}_${setup.wordsPerGame}_${setup.lives}" + if (setup.single) "_single" else "_void"
}
