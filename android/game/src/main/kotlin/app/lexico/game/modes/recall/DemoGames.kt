package app.lexico.game.modes.recall

import app.lexico.engine.WooglesEngine
import app.lexico.game.engine.engine
import app.lexico.game.engine.parseScoredPlacements
import app.lexico.go.demo.Demo

data class ScoredPlacement(val placement: String, val score: Int)

class DemoGames internal constructor() {
  suspend fun next(): List<ScoredPlacement> =
    engine { parseScoredPlacements(Demo.playWithScores()).map { ScoredPlacement(it.placement, it.score) } }

  suspend fun nextPlacements(): List<String> = next().map { it.placement }

  fun isWord(word: String): Boolean = WooglesEngine.isValidWord(word)
}
