package app.lexico.games.export

import java.io.OutputStream

internal class LzwEncoder(private val out: OutputStream) {
  private val table = HashMap<Int, Int>()
  private val block = ByteArray(BLOCK)
  private var blockSize = 0
  private var bits = 0
  private var bitCount = 0
  private var width = LITERAL_BITS + 1
  private var hi = END
  private var overflow = CLEAR shl 1

  fun encode(indices: ByteArray) {
    out.write(LITERAL_BITS)
    emit(CLEAR)
    var code = indices[0].toInt() and 0xFF
    for (i in 1 until indices.size) code = step(code, indices[i].toInt() and 0xFF)
    emit(code)
    nextCode()
    emit(END)
    flush()
  }

  private fun step(code: Int, literal: Int): Int {
    val key = (code shl 8) or literal
    table[key]?.let { return it }
    emit(code)
    if (nextCode()) table[key] = hi
    return literal
  }

  private fun nextCode(): Boolean {
    hi++
    if (hi == overflow) {
      width++
      overflow = overflow shl 1
    }
    if (hi < MAX_CODE) return true
    emit(CLEAR)
    reset()
    return false
  }

  private fun reset() {
    width = LITERAL_BITS + 1
    hi = END
    overflow = CLEAR shl 1
    table.clear()
  }

  private fun emit(code: Int) {
    bits = bits or (code shl bitCount)
    bitCount += width
    while (bitCount >= 8) {
      put(bits and 0xFF)
      bits = bits ushr 8
      bitCount -= 8
    }
  }

  private fun put(byte: Int) {
    block[blockSize++] = byte.toByte()
    if (blockSize == BLOCK) writeBlock()
  }

  private fun flush() {
    if (bitCount > 0) put(bits and 0xFF)
    if (blockSize > 0) writeBlock()
    out.write(0)
  }

  private fun writeBlock() {
    out.write(blockSize)
    out.write(block, 0, blockSize)
    blockSize = 0
  }

  private companion object {
    const val LITERAL_BITS = 8
    const val CLEAR = 1 shl LITERAL_BITS
    const val END = CLEAR + 1
    const val MAX_CODE = (1 shl 12) - 1
    const val BLOCK = 255
  }
}
