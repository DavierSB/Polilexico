package app.lexico.games.export

internal class Palette private constructor(private val colors: IntArray) {
  private val lookup = IntArray(BUCKETS) { -1 }

  fun rgb(): ByteArray = ByteArray(SIZE * 3) { channel(colors.getOrElse(it / 3) { 0 }, 2 - it % 3).toByte() }

  fun indices(pixels: IntArray): ByteArray = ByteArray(pixels.size) { index(pixels[it]).toByte() }

  private fun index(argb: Int): Int {
    val bucket = bucket(argb)
    if (lookup[bucket] < 0) lookup[bucket] = nearest(argb)
    return lookup[bucket]
  }

  private fun nearest(argb: Int): Int = colors.indices.minBy { distance(colors[it], argb) }

  private fun distance(a: Int, b: Int): Int = (0..2).sumOf { (channel(a, it) - channel(b, it)).let { d -> d * d } }

  companion object {
    private const val SIZE = 256
    private const val BUCKETS = 1 shl 15

    fun of(frames: List<IntArray>): Palette {
      val counts = IntArray(BUCKETS)
      val sums = LongArray(BUCKETS * 3)
      frames.forEach { pixels -> pixels.forEach { add(it, counts, sums) } }
      val top = counts.indices.filter { counts[it] > 0 }.sortedByDescending { counts[it] }.take(SIZE)
      return Palette(IntArray(top.size) { average(top[it], counts, sums) })
    }

    private fun add(argb: Int, counts: IntArray, sums: LongArray) {
      val bucket = bucket(argb)
      counts[bucket]++
      for (c in 0..2) sums[bucket * 3 + c] += channel(argb, c).toLong()
    }

    private fun average(bucket: Int, counts: IntArray, sums: LongArray): Int =
      (0..2).fold(0) { color, c -> color or ((sums[bucket * 3 + c] / counts[bucket]).toInt() shl (8 * c)) }

    private fun bucket(argb: Int): Int = (0..2).fold(0) { key, c -> key or ((channel(argb, c) shr 3) shl (5 * c)) }

    private fun channel(argb: Int, c: Int): Int = argb shr (8 * c) and 0xFF
  }
}
