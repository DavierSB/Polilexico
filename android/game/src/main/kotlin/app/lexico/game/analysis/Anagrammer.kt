package app.lexico.game.analysis

import app.lexico.engine.WooglesEngine
import app.lexico.game.engine.engine
import app.lexico.game.engine.rackText
import app.lexico.go.engine.Engine
import app.lexico.model.Letters

class Anagrammer internal constructor() {
  suspend fun anagrams(tiles: List<String>): List<String> = words(engine { Engine.anagrams(rackText(tiles)) })

  suspend fun anagramTiles(tiles: List<String>): List<List<String>> = words(engine { Engine.anagramTiles(rackText(tiles)) }).map(::tilesOf)

  suspend fun randomWord(minTiles: Int, maxTiles: Int): List<String> =
    tilesOf(engine { Engine.randomWord(minTiles.toLong(), maxTiles.toLong()) })

  fun isWord(word: String): Boolean = WooglesEngine.isValidWord(word)

  private fun words(text: String): List<String> = text.split(" ").filter { it.isNotEmpty() }

  private fun tilesOf(word: String): List<String> = Letters.tiles(word).mapNotNull { it?.letter?.uppercase() }
}
