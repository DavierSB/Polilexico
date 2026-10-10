package app.lexico.ui.common

private const val FULL = 100f

data class Efficiency(val turns: List<Float?>, val running: List<Float>) {
  val lastIndex: Int get() = turns.lastIndex

  fun isHit(i: Int): Boolean = turns[i] == FULL

  companion object {
    fun of(mine: List<Int>, master: List<Int>): Efficiency {
      val turns = mine.zip(master) { m, best -> if (best > 0) minOf(FULL, m * FULL / best) else null }
      return Efficiency(turns, running(mine, master))
    }

    private fun running(mine: List<Int>, master: List<Int>): List<Float> {
      var got = 0
      var best = 0
      return mine.zip(master) { m, b ->
        got += m
        best += b
        if (best > 0) got * FULL / best else FULL
      }
    }
  }
}
