package app.lexico.game.records

import android.content.Context
import app.lexico.game.modes.sprint.SprintSetup

class SprintRecords(context: Context) {
  private val store = RecordStore(context, "records_sprint")

  fun best(setup: SprintSetup): Int = store.best(key(setup))

  fun submit(setup: SprintSetup, solved: Int): SeriesRecord = store.submit(key(setup), solved)

  private fun key(setup: SprintSetup): String = "${setup.totalMs}_${setup.lives}" + if (setup.invalidCostsLife) "_single" else ""
}
