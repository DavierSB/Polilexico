package app.lexico.game.records

import app.lexico.game.engine.EngineListener
import app.lexico.game.engine.engine
import app.lexico.game.engine.engineMessage
import app.lexico.game.engine.parseBoard
import app.lexico.game.engine.rackTiles
import app.lexico.game.engine.stoppingOnCancel
import app.lexico.go.review.Analysis
import app.lexico.go.review.Explorer
import app.lexico.model.Board
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val ANALYSIS_BUDGET_MS = 30_000L
private const val UPDATE_GAP_MS = 150L
private const val PASS = "pasar"

class EndgameExplorer internal constructor(private val explorer: Explorer, private val start: Board) {
  suspend fun position(): ExplorerPosition = engine {
    ExplorerPosition(
      board = parseBoard(explorer.board()), racks = players { rackTiles(explorer.rack(it)) }, scores = players { explorer.score(it).toInt() },
      onTurn = explorer.onTurn().toInt(), over = explorer.over(), path = path(), ending = ending(explorer.ending()),
    )
  }

  suspend fun play(move: String?): String? = engine {
    runCatching { explorer.play(move ?: PASS) }.exceptionOrNull()?.engineMessage()
  }

  suspend fun back(steps: Int = 1) = engine { repeat(steps) { runCatching { explorer.back() } } }

  suspend fun analyze(update: suspend (EndgameReview) -> Unit) {
    val analysis = engine { explorer.analysis(ANALYSIS_BUDGET_MS) } ?: return
    val board = engine { parseBoard(explorer.board()) }
    coroutineScope {
      val changes = Channel<Unit>(Channel.CONFLATED)
      launch { run(analysis, changes) }
      for (change in changes) {
        update(latest(analysis, board))
        delay(UPDATE_GAP_MS)
      }
      update(latest(analysis, board))
    }
  }

  private suspend fun run(analysis: Analysis, changes: Channel<Unit>) {
    val listener = EngineListener().apply { target = { changes.trySend(Unit) } }
    try {
      stoppingOnCancel(analysis::stop) { engine { analysis.run(listener) } }
    } finally {
      changes.close()
    }
  }

  private suspend fun latest(analysis: Analysis, board: Board): EndgameReview = engine { endgameReview(board, analysis.latest()) }

  private fun path(): List<LineStep> {
    val moves = (0 until explorer.stepCount()).map { explorer.stepAt(it) }
    return steps(start, moves.map { it.description to it.score.toInt() })
  }

  private fun <T> players(of: (Long) -> T): List<T> = listOf(of(0), of(1))
}
