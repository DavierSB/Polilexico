package app.lexico.modes.recall

import app.lexico.game.Lexico
import app.lexico.game.modes.recall.RecallSetup
import app.lexico.game.modes.recall.ScoredPlacement as GameScoredPlacement
import app.lexico.game.race.RaceGame
import app.lexico.game.race.RaceSetup
import app.lexico.game.records.RecallRecords
import app.lexico.ui.recall.GameSource
import app.lexico.ui.recall.RecallConfig
import app.lexico.ui.recall.RecallRace
import app.lexico.ui.recall.Record
import app.lexico.ui.recall.RecordBook
import app.lexico.ui.recall.ScoredPlacement
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import java.util.concurrent.atomic.AtomicInteger

fun engineGames(lexico: Lexico): GameSource = object : GameSource {
  override suspend fun game(): List<ScoredPlacement> =
    lexico.demoGames.next().map { ScoredPlacement(it.placement, it.score) }

  override fun isWord(word: String): Boolean = lexico.demoGames.isWord(word)
}

fun RecallConfig.toSetup(): RecallSetup = RecallSetup(intervalMs, wordsPerGame, lives, single)

fun recallRecords(records: RecallRecords, config: RecallConfig): RecordBook = object : RecordBook {
  private val setup = config.toSetup()

  override fun best(): Int = records.best(setup)

  override fun submit(recalled: Int): Record = records.submit(setup, recalled).let { Record(it.best, it.isNew) }
}

fun RecallRace.toRace(): RaceSetup =
  RaceSetup(RaceGame.RECALL, goal = goal, intervalMs = config.intervalMs, single = config.single, roundMs = roundMs)

fun RaceSetup.toRecallConfig(): RecallConfig = RecallConfig(intervalMs, lives = RACE_LIVES, single = single, rounds = rounds, shared = true)

fun sharedGames(lexico: Lexico): SharedGames = SharedGames(lexico)

class SharedGames internal constructor(private val lexico: Lexico) : GameSource {
  private val rounds = MutableStateFlow<Map<Int, List<ScoredPlacement>>>(emptyMap())
  private val asked = AtomicInteger()

  fun offer(round: Int, game: List<GameScoredPlacement>) {
    rounds.update { it + (round to game.map { p -> ScoredPlacement(p.placement, p.score) }) }
  }

  override suspend fun game(): List<ScoredPlacement> {
    val round = asked.incrementAndGet()
    return rounds.first { round in it }.getValue(round)
  }

  override fun isWord(word: String): Boolean = lexico.demoGames.isWord(word)
}

private const val RACE_LIVES = 3
