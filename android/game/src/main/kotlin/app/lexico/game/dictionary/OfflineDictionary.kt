package app.lexico.game.dictionary

import android.content.Context
import app.lexico.model.dictionary.Dictionary
import app.lexico.model.dictionary.Meaning
import java.io.File
import org.tukaani.xz.XZInputStream

class OfflineDictionary internal constructor(private val context: Context) {
  private val dictionary: Dictionary by lazy { Dictionary(unpacked()) }

  fun meanings(word: String): List<Meaning> = runCatching { dictionary.meanings(word) }.getOrDefault(emptyList())

  fun open() {
    runCatching { dictionary }
  }

  private fun unpacked(): File {
    val dir = File(context.filesDir, DIRECTORY)
    val stamp = File(dir, STAMP)
    if (stamp.takeIf(File::exists)?.readText() != version()) unpack(dir, stamp)
    return dir
  }

  private fun unpack(dir: File, stamp: File) {
    dir.deleteRecursively()
    dir.mkdirs()
    PARTS.forEach { (asset, file) -> extract("$DIRECTORY/$asset", File(dir, file)) }
    stamp.writeText(version())
  }

  private fun extract(asset: String, target: File) {
    val temporary = File(target.path + ".tmp")
    XZInputStream(context.assets.open(asset).buffered()).use { input -> temporary.outputStream().use { input.copyTo(it, 1 shl 16) } }
    temporary.renameTo(target)
  }

  private fun version(): String = PARTS.joinToString(",") { (asset, _) -> context.assets.openFd("$DIRECTORY/$asset").use { it.length }.toString() }

  private companion object {
    const val DIRECTORY = "dictionary"
    const val STAMP = "version"
    val PARTS = listOf(
      "words.xz" to Dictionary.WORDS, "senses.xz" to Dictionary.SENSES, "relations.xz" to Dictionary.RELATIONS,
      "demonyms.xz" to Dictionary.DEMONYMS,
    )
  }
}
