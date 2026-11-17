package app.lexico.ui.games

import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

private const val ROW_CAPACITY = 5

internal data class StripFrame(val left: Float, val right: Float, val bottom: Float, val step: Float, val labelHeight: Float)

internal data class DotSpot(val x: Float, val y: Float)

internal data class GroupLabel(val low: Int, val high: Int, val x: Float, val bottom: Float)

internal class DotLayout(val spots: List<DotSpot>, val labels: List<GroupLabel>)

internal fun layoutDots(xs: List<Float>, values: List<Int>, frame: StripFrame): DotLayout {
  val groups = DotGroups(xs, frame)
  val placed = groups.all.map { groups.place(it) }
  val spots = placed.flatten().sortedBy { it.first }.map { it.second }
  val labels = groups.all.zip(placed).filter { it.first.size > 1 }.map { (g, p) -> labelOf(g, p, values, frame) }
  return DotLayout(spots, labels)
}

private fun labelOf(group: List<Int>, placed: List<Pair<Int, DotSpot>>, values: List<Int>, frame: StripFrame): GroupLabel {
  val top = placed.minOf { it.second.y } - frame.step / 2
  val x = (placed.minOf { it.second.x } + placed.maxOf { it.second.x }) / 2
  return GroupLabel(group.minOf { values[it] }, group.maxOf { values[it] }, x, top)
}

private class DotGroups(private val xs: List<Float>, private val frame: StripFrame) {
  private val maxRows = max(1, ((frame.bottom - frame.labelHeight) / frame.step).toInt())
  val all: List<List<Int>> = merge(xs.indices.sortedWith(compareBy({ xs[it] }, { it })).map { listOf(it) })

  fun place(group: List<Int>): List<Pair<Int, DotSpot>> {
    val centre = clampedCentre(group)
    return group.mapIndexed { k, i -> i to spot(k, group.size, centre) }
  }

  private fun merge(groups: List<List<Int>>): List<List<Int>> {
    val merged = groups.fold(mutableListOf<List<Int>>()) { acc, g -> acc.also { join(it, g) } }
    return if (merged.size == groups.size) merged else merge(merged)
  }

  private fun join(acc: MutableList<List<Int>>, group: List<Int>) {
    val last = acc.lastOrNull()
    if (last != null && touches(last, group)) acc[acc.lastIndex] = last + group else acc += group
  }

  private fun touches(a: List<Int>, b: List<Int>): Boolean =
    (centre(b) - halfWidth(b.size)) - (centre(a) + halfWidth(a.size)) < frame.step

  private fun spot(k: Int, n: Int, centre: Float): DotSpot {
    val row = k / perRow(n)
    val inRow = min(perRow(n), n - row * perRow(n))
    val x = centre + (k % perRow(n) - (inRow - 1) / 2f) * hStep(n)
    return DotSpot(x, frame.bottom - frame.step / 2 - row * frame.step)
  }

  private fun clampedCentre(group: List<Int>): Float {
    val half = halfWidth(group.size)
    val low = frame.left + frame.step / 2 + half
    val high = frame.right - frame.step / 2 - half
    return if (low > high) (frame.left + frame.right) / 2 else centre(group).coerceIn(low, high)
  }

  private fun centre(group: List<Int>): Float = (xs[group.first()] + xs[group.last()]) / 2

  private fun halfWidth(n: Int): Float = (min(n, perRow(n)) - 1) / 2f * hStep(n)

  private fun perRow(n: Int): Int = max(ROW_CAPACITY, ceil(n / maxRows.toFloat()).toInt())

  private fun hStep(n: Int): Float {
    val fit = (frame.right - frame.left - frame.step) / max(1, perRow(n) - 1)
    return min(frame.step, fit)
  }
}
