package app.lexico.modes.recall

import app.lexico.game.Lexico
import app.lexico.ui.recall.GameSource
import app.lexico.ui.recall.ScoredPlacement

/**
 * Las partidas de "¿Cuántas recuerdas?": HastyBot contra si mismo, del motor, que tambien dice
 * que fichas fijas pueden ir pegadas.
 */
fun engineGames(lexico: Lexico): GameSource = object : GameSource {
  override suspend fun game(): List<ScoredPlacement> =
    lexico.demoGames.next().map { ScoredPlacement(it.placement, it.score) }

  override fun isWord(word: String): Boolean = lexico.demoGames.isWord(word)
}
