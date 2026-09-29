package app.lexico.game.records

import android.content.Context
import app.lexico.game.modes.sprint.SprintSetup

/** El record de Scrabble Sprint (manos resueltas) de cada combinacion de tiempo, vidas y comprobacion. */
class SprintRecords(context: Context) {
  private val prefs = context.getSharedPreferences("records_sprint", Context.MODE_PRIVATE)

  /** El record con estas opciones; 0 si aun no hay. */
  fun best(setup: SprintSetup): Int = prefs.getInt(key(setup), 0)

  /** Anota una serie terminada: el record despues de anotarla y si esta lo supero. */
  fun submit(setup: SprintSetup, solved: Int): SprintRecord {
    val previous = best(setup)
    if (solved > previous) prefs.edit().putInt(key(setup), solved).apply()
    return SprintRecord(maxOf(previous, solved), isNew = solved > previous)
  }

  // Las series void guardan la clave de antes de poder elegir: sus records siguen valiendo.
  private fun key(setup: SprintSetup): String = "${setup.totalMs}_${setup.lives}" + if (setup.invalidCostsLife) "_single" else ""
}

data class SprintRecord(val best: Int, val isNew: Boolean)
