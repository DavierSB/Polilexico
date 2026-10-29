package app.lexico.games.export

import java.io.OutputStream

internal class GifFrame(val pixels: IntArray, val delayCs: Int)

internal class GifEncoder(private val width: Int, private val height: Int) {
  fun write(frames: List<GifFrame>, out: OutputStream) {
    val palette = Palette.of(frames.map { it.pixels })
    header(out, palette)
    frames.fold(null as ByteArray?) { previous, f -> palette.indices(f.pixels).also { frame(out, it, previous, f.delayCs) } }
    out.write(TRAILER)
    out.flush()
  }

  private fun header(out: OutputStream, palette: Palette) {
    out.write("GIF89a".toByteArray())
    out.short(width)
    out.short(height)
    out.write(byteArrayOf(0xF7.toByte(), 0, 0))
    out.write(palette.rgb())
    out.write(byteArrayOf(0x21, 0xFF.toByte(), 0x0B))
    out.write("NETSCAPE2.0".toByteArray())
    out.write(byteArrayOf(3, 1, 0, 0, 0))
  }

  private fun frame(out: OutputStream, indices: ByteArray, previous: ByteArray?, delayCs: Int) {
    val area = changedArea(indices, previous)
    out.write(byteArrayOf(0x21, 0xF9.toByte(), 4, 4))
    out.short(delayCs)
    out.write(byteArrayOf(0, 0))
    out.write(0x2C)
    listOf(area.x, area.y, area.width, area.height).forEach { out.short(it) }
    out.write(0)
    LzwEncoder(out).encode(crop(indices, area))
  }

  private fun changedArea(indices: ByteArray, previous: ByteArray?): Area {
    if (previous == null) return Area(0, 0, width, height)
    val changed = indices.indices.filter { indices[it] != previous[it] }
    if (changed.isEmpty()) return Area(0, 0, 1, 1)
    val xs = changed.map { it % width }
    val ys = changed.map { it / width }
    return Area(xs.min(), ys.min(), xs.max() - xs.min() + 1, ys.max() - ys.min() + 1)
  }

  private fun crop(indices: ByteArray, area: Area): ByteArray =
    ByteArray(area.width * area.height) { indices[(area.y + it / area.width) * width + area.x + it % area.width] }

  private fun OutputStream.short(value: Int) {
    write(value and 0xFF)
    write(value shr 8 and 0xFF)
  }

  private data class Area(val x: Int, val y: Int, val width: Int, val height: Int)

  private companion object {
    const val TRAILER = 0x3B
  }
}
