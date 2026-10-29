package app.lexico.ui.lists

import java.util.Locale

object WordKeys {
  private val SPELLED = mapOf('C' to "ch", 'L' to "ll", 'R' to "rr")
  private val DIGRAPHS = mapOf("ch" to 'C', "ll" to 'L', "rr" to 'R')
  private const val WORD = "w:"
  private val ACCENTS = mapOf('á' to 'a', 'é' to 'e', 'í' to 'i', 'ó' to 'o', 'ú' to 'u', 'ü' to 'u')

  fun show(key: String): String = buildString { key.forEach { append(SPELLED[it] ?: it) } }

  fun upper(key: String): String = show(key).uppercase(Locale.ROOT)

  fun label(tile: Char): String = upper(tile.toString())

  fun tiles(key: String): List<String> = key.map(::label)

  fun of(tiles: List<String>): String = tiles.joinToString("") { tile -> keyOf(tile.lowercase(Locale.ROOT)) }

  fun alphabetic(key: String): String = show(key).replace("ñ", "n~")

  fun entry(text: String): String = if (':' in text) text else wordEntry(parse(text))

  fun wordEntry(key: String): String = WORD + key

  fun bare(entry: String): String = entry.substringAfter(':')

  fun parse(text: String): String {
    val lower = text.lowercase(Locale.ROOT)
    val out = StringBuilder()
    var i = 0
    while (i < lower.length) i += append(out, lower, i)
    return out.toString()
  }

  private fun keyOf(tile: String): String = DIGRAPHS[tile]?.toString() ?: tile

  private fun append(out: StringBuilder, text: String, at: Int): Int {
    val digraph = DIGRAPHS[text.substring(at, minOf(at + 2, text.length))]
    if (digraph != null) out.append(digraph) else letter(text[at])?.let(out::append)
    return if (digraph != null) 2 else 1
  }

  private fun letter(c: Char): Char? = ACCENTS[c] ?: c.takeIf { it in 'a'..'z' || it == 'ñ' }
}
