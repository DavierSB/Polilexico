package app.lexico.game.records

import android.content.Context
import app.lexico.game.modes.recall.RecallSetup

/** El record de "¿Cuántas recuerdas?" (palabras recordadas) de cada combinacion de opciones. */
class RecallRecords(context: Context) {
  private val store = RecordStore(context, "records_recall")

  /** El record con estas opciones; 0 si aun no hay. */
  fun best(setup: RecallSetup): Int = store.best(key(setup))

  /** Anota una serie terminada: el record despues de anotarla y si esta lo supero. */
  fun submit(setup: RecallSetup, recalled: Int): SeriesRecord = store.submit(key(setup), recalled)

  private fun key(setup: RecallSetup): String =
    "${setup.intervalMs}_${setup.wordsPerGame}_${setup.lives}" + if (setup.single) "_single" else "_void"
}
