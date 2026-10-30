package app.lexico.game.modes.duplicate.table

import app.lexico.game.engine.EngineListener
import app.lexico.game.engine.engine
import app.lexico.game.engine.engineMessage
import app.lexico.go.duplicate.Duplicate
import app.lexico.go.duplicate.Table
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

data class TableSetup(val turnMs: Long, val rounds: Int, val live: Boolean)

class DuplicateTable private constructor(private val table: Table, val players: List<String>) {
  private val _changes = MutableSharedFlow<Unit>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
  val changes: SharedFlow<Unit> = _changes

  suspend fun view(player: String): String = engine { table.view(player) }

  suspend fun propose(player: String, input: String): String? =
    engine { runCatching { table.propose(player, input) }.exceptionOrNull()?.engineMessage() }

  suspend fun ready(player: String) = engine { table.ready(player) }

  suspend fun setPresent(player: String, present: Boolean) = engine { table.setPresent(player, present) }

  fun close() = table.close()

  companion object {
    suspend fun open(players: List<String>, setup: TableSetup): DuplicateTable {
      val listener = EngineListener()
      val table = engine { Duplicate.newTable(players.joinToString("\n"), setup.turnMs, setup.rounds.toLong(), setup.live, listener) }
      return DuplicateTable(table, players).also { t ->
        listener.target = { t._changes.tryEmit(Unit) }
        t._changes.tryEmit(Unit)
      }
    }
  }
}
