package app.lexico.ui.common

data class TrackSpot(val x: Float, val runners: List<RaceRunner>, val label: String, val labelX: Float, val above: Boolean)

object TrackLayout {
  fun of(runners: List<RaceRunner>, goal: Int, width: Float, dot: Float, gap: Float, measure: (String) -> Float): List<TrackSpot> {
    var clusters = byScore(runners).map { listOf(it) }.let { merged(it, goal, width, dot) }
    while (true) {
      val spots = placed(clusters, goal, width, gap, measure)
      val clash = spots.indexOfFirst { it == null }
      if (clash < 0) return spots.filterNotNull()
      clusters = clusters.take(clash - 1) + listOf(clusters[clash - 1] + clusters[clash]) + clusters.drop(clash + 1)
    }
  }

  private fun byScore(runners: List<RaceRunner>): List<List<RaceRunner>> =
    runners.groupBy { it.score }.toSortedMap().values.toList()

  private fun merged(groups: List<List<List<RaceRunner>>>, goal: Int, width: Float, dot: Float): List<List<List<RaceRunner>>> =
    groups.fold(mutableListOf()) { acc, g ->
      val last = acc.lastOrNull()
      if (last != null && xOf(g, goal, width) - xOf(last, goal, width) < dot) acc[acc.lastIndex] = last + g else acc += g
      acc
    }

  private fun placed(clusters: List<List<List<RaceRunner>>>, goal: Int, width: Float, gap: Float, measure: (String) -> Float): List<TrackSpot?> {
    val ends = mutableMapOf(true to Float.NEGATIVE_INFINITY, false to Float.NEGATIVE_INFINITY)
    var preferAbove = true
    return clusters.map { cluster ->
      val runners = cluster.flatten()
      val x = xOf(cluster, goal, width)
      val label = labelOf(runners, width, measure)
      val w = measure(label)
      val left = (x - w / 2).coerceIn(0f, maxOf(0f, width - w))
      val row = listOf(preferAbove, !preferAbove).firstOrNull { left >= ends.getValue(it) + gap } ?: return@map null
      ends[row] = left + w
      preferAbove = !row
      TrackSpot(x, runners, label, left, row)
    }
  }

  private fun labelOf(runners: List<RaceRunner>, width: Float, measure: (String) -> Float): String {
    if (runners.size == 1) return runners[0].name
    val names = runners.joinToString(", ") { it.name }
    return if (measure(names) <= width / 2) names else "${runners.size} jugadores"
  }

  private fun xOf(cluster: List<List<RaceRunner>>, goal: Int, width: Float): Float {
    val scores = cluster.flatten().map { it.score.coerceAtMost(goal) }
    return width * scores.average().toFloat() / goal
  }
}
