package app.lexico.game.records

import android.content.Context
import app.lexico.game.modes.duplicate.DuplicateSetup

class DuplicateRecords(context: Context) {
  private val store = RecordStore(context, "records_duplicate")

  fun best(setup: DuplicateSetup): Int = store.best(key(setup))

  fun submit(setup: DuplicateSetup, efficiencyTenths: Int): SeriesRecord = store.submit(key(setup), efficiencyTenths)

  private fun key(setup: DuplicateSetup): String =
    "${setup.maxRounds}_${setup.turnMs}" + if (setup.invalidLosesTurn) "_single" else "_void"
}
