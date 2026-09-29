package app.lexico.ui.board

import kotlin.random.Random

/** Que fichas se van del atril entre un turno y el siguiente; con ello se ordena el atril nuevo. */
enum class RackRenewal {
  /** Clasica: se van las que juegas o cambias; las demas se quedan. */
  MY_PLAYS,

  /** Duplicada: se van las de la jugada del master; se quedan las que coinciden con el atril nuevo. */
  MASTER_PLAYS,

  /** Sprint: cada mano es un atril nuevo entero. */
  WHOLE,
}

/**
 * El orden en pantalla de un atril nuevo (indices en `rack`): primero las fichas que se quedan
 * (`staying`, en el orden en que se veian) y detras las recien robadas, barajadas.
 */
internal fun arrangeRack(staying: List<String>, rack: List<String>, random: Random = Random): List<Int> {
  val drawn = rack.indices.toMutableList()
  val kept = staying.mapNotNull { letter -> drawn.firstOrNull { rack[it] == letter }?.also { drawn.remove(it) } }
  return kept + drawn.shuffled(random)
}
