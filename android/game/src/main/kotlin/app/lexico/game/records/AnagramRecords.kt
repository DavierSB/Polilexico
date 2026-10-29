package app.lexico.game.records

import android.content.Context
import app.lexico.game.modes.anagram.AnagramSetup

class AnagramRecords(context: Context) {
  private val store = RecordStore(context, "records_anagram")

  fun best(setup: AnagramSetup): Int = store.best(key(setup))

  fun submit(setup: AnagramSetup, solved: Int): SeriesRecord = store.submit(key(setup), solved)

  private fun key(setup: AnagramSetup): String = "${setup.totalMs}_${setup.lives}_${setup.difficulty}"
}
