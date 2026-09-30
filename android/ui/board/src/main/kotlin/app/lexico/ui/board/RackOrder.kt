package app.lexico.ui.board

import kotlin.random.Random

enum class RackRenewal {
  MY_PLAYS,

  MASTER_PLAYS,

  WHOLE,
}

internal fun arrangeRack(staying: List<String>, rack: List<String>, random: Random = Random): List<Int> {
  val drawn = rack.indices.toMutableList()
  val kept = staying.mapNotNull { letter -> drawn.firstOrNull { rack[it] == letter }?.also { drawn.remove(it) } }
  return kept + drawn.shuffled(random)
}
