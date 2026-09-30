package app.lexico.game.records

import android.content.Context

/** Los records de un minijuego en el telefono: el mejor numero de cada combinacion de opciones (`key`). */
internal class RecordStore(context: Context, name: String) {
  private val prefs = context.getSharedPreferences(name, Context.MODE_PRIVATE)

  /** El record de `key`; 0 si aun no hay. */
  fun best(key: String): Int = prefs.getInt(key, 0)

  /** Anota una serie terminada: el record despues de anotarla y si esta lo supero. */
  fun submit(key: String, score: Int): SeriesRecord {
    val previous = best(key)
    if (score > previous) prefs.edit().putInt(key, score).apply()
    return SeriesRecord(maxOf(previous, score), isNew = score > previous)
  }
}

data class SeriesRecord(val best: Int, val isNew: Boolean)
