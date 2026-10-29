package app.lexico.game.lists

import android.content.Context

class WordData internal constructor(private val context: Context) {
  fun families(): List<String> = lines("families.tsv")

  fun words(): List<String> = lines("words.tsv")

  fun similar(): List<String> = lines("similar.tsv")

  fun defaultLists(): List<String> = lines("default_lists.tsv")

  private fun lines(name: String): List<String> = context.assets.open("lexicon/$name").bufferedReader().use { it.readLines() }
}
