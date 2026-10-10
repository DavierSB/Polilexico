package app.lexico.ui.common

import kotlin.math.abs
import kotlin.math.ceil

private const val STEP = 50
private const val MAX_LINES = 3
private val GRID_STEPS = listOf(50, 100, 200, 250, 500, 1000)

data class Spread(val top: String, val bottom: String, val points: List<Int>) {
  val limit: Int get() = (ceil((points.maxOfOrNull(::abs) ?: 0) / STEP.toDouble()).toInt() * STEP).coerceAtLeast(STEP)

  val lastIndex: Int get() = points.lastIndex

  val gridLines: List<Int> get() = (GRID_STEPS.firstOrNull { limit / it <= MAX_LINES } ?: limit).let { step -> (step until limit step step).toList() }

  companion object {
    fun of(me: String, opponent: String, meStarts: Boolean, totals: List<Pair<Int, Int>>): Spread {
      val lead = totals.map { it.first - it.second }
      return if (meStarts) Spread(me, opponent, lead) else Spread(opponent, me, lead.map { -it })
    }
  }
}
