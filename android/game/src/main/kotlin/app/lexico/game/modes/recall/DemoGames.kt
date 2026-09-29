package app.lexico.game.modes.recall

import app.lexico.engine.WooglesEngine
import app.lexico.game.engine.engine
import app.lexico.game.engine.parseScoredPlacements
import app.lexico.go.demo.Demo

/** Una colocacion de una partida de demostracion ("H8 CA.A") con sus puntos. */
data class ScoredPlacement(val placement: String, val score: Int)

/** Partidas de HastyBot contra si mismo, para "¿Cuántas recuerdas?" y el tablero del inicio. */
class DemoGames internal constructor() {
  /** Una partida nueva, con los puntos de cada colocacion. */
  suspend fun next(): List<ScoredPlacement> =
    engine { parseScoredPlacements(Demo.playWithScores()).map { ScoredPlacement(it.placement, it.score) } }

  /** Una partida nueva, solo sus colocaciones ("H8 CA.A"). */
  suspend fun nextPlacements(): List<String> = next().map { it.placement }

  /** Si las fichas juntas son una palabra de FILE2017 ("[CH]E"). Bloquea: fuera del hilo principal. */
  fun isWord(word: String): Boolean = WooglesEngine.isValidWord(word)
}
