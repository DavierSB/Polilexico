package app.lexico.modes.anagram

import app.lexico.game.Lexico
import app.lexico.game.modes.anagram.AnagramSetup
import app.lexico.game.race.RaceGame
import app.lexico.game.race.RaceSetup
import app.lexico.game.records.AnagramRecords
import app.lexico.ui.anagram.AnagramConfig
import app.lexico.ui.anagram.AnagramRace
import app.lexico.ui.anagram.AnagramSource
import app.lexico.ui.anagram.Difficulty
import app.lexico.ui.common.RACE_LIVES
import app.lexico.ui.recall.Record
import app.lexico.ui.recall.RecordBook

fun engineAnagramSource(lexico: Lexico): AnagramSource = object : AnagramSource {
  override suspend fun randomWord(minTiles: Int, maxTiles: Int): List<String> = lexico.anagrammer.randomWord(minTiles, maxTiles)

  override suspend fun anagrams(tiles: List<String>): List<List<String>> = lexico.anagrammer.anagramTiles(tiles)

  override fun isWord(word: String): Boolean = lexico.anagrammer.isWord(word)
}

fun AnagramConfig.toSetup(): AnagramSetup = AnagramSetup(totalMs, lives, difficulty.key)

fun anagramRecords(records: AnagramRecords, config: AnagramConfig): RecordBook = object : RecordBook {
  private val setup = config.toSetup()

  override fun best(): Int = records.best(setup)

  override fun submit(recalled: Int): Record = records.submit(setup, recalled).let { Record(it.best, it.isNew) }
}

fun AnagramRace.toRace(): RaceSetup = RaceSetup(RaceGame.ANAGRAM, timeMs = config.totalMs, goal = goal, difficulty = config.difficulty.key)

fun RaceSetup.toAnagramConfig(totalMs: Long): AnagramConfig =
  AnagramConfig(totalMs, RACE_LIVES, Difficulty.entries.first { it.key == difficulty })
