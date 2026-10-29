package app.lexico.model.dictionary

import java.io.File
import java.io.RandomAccessFile

class SortedLines(file: File) {
  private val reader = RandomAccessFile(file, "r")
  private val starts: LongArray = lineStarts(file)

  fun find(key: String): String? = synchronized(reader) {
    var low = 0
    var high = starts.size - 2
    while (low <= high) {
      val middle = (low + high) ushr 1
      val line = line(middle)
      val order = keyOf(line).compareTo(key)
      if (order == 0) return line
      if (order < 0) low = middle + 1 else high = middle - 1
    }
    null
  }

  private fun line(index: Int): String {
    val bytes = ByteArray((starts[index + 1] - starts[index] - 1).toInt())
    reader.seek(starts[index])
    reader.readFully(bytes)
    return String(bytes, Charsets.UTF_8)
  }

  private fun keyOf(line: String): String = line.substringBefore('\t')

  private companion object {
    fun lineStarts(file: File): LongArray {
      val starts = mutableListOf(0L)
      val buffer = ByteArray(1 shl 16)
      var offset = 0L
      file.inputStream().use { input ->
        var read = input.read(buffer)
        while (read > 0) {
          for (i in 0 until read) if (buffer[i] == NEWLINE) starts += offset + i + 1
          offset += read
          read = input.read(buffer)
        }
      }
      return starts.toLongArray()
    }

    const val NEWLINE = '\n'.code.toByte()
  }
}
