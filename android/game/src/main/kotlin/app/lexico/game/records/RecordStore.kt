package app.lexico.game.records

import android.content.Context

internal class RecordStore(context: Context, name: String) {
  private val prefs = context.getSharedPreferences(name, Context.MODE_PRIVATE)

  fun best(key: String): Int = prefs.getInt(key, 0)

  fun submit(key: String, score: Int): SeriesRecord {
    val previous = best(key)
    if (score > previous) prefs.edit().putInt(key, score).apply()
    return SeriesRecord(maxOf(previous, score), isNew = score > previous)
  }
}

data class SeriesRecord(val best: Int, val isNew: Boolean)
