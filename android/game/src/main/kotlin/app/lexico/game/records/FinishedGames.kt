package app.lexico.game.records

import app.lexico.game.Outcome
import app.lexico.game.engine.engine
import app.lexico.game.engine.parseBoard
import app.lexico.game.storage.Mode
import app.lexico.go.review.Game
import app.lexico.go.review.Review
import java.io.File

class FinishedGames internal constructor(private val dir: File) {
  suspend fun list(): List<FinishedGame> = engine {
    logFiles().mapNotNull { f -> runCatching { summary(f.path, Review.open(f.path)).copy(finishedAt = f.lastModified()) }.getOrNull() }
  }

  suspend fun open(path: String): GameReview = engine {
    val game = Review.open(path)
    GameReview(summary(path, game), (0 until game.turnCount()).map { reviewTurn(game.turnAt(it)) }, game.startTurn.toInt())
  }

  suspend fun explore(path: String, turn: Int): EndgameExplorer = engine {
    val t = Review.open(path).turnAt(turn.toLong())
    EndgameExplorer(t.explore(), parseBoard(t.board))
  }

  suspend fun preEndgame(path: String, turn: Int): PreEndgameAnalyzer = engine {
    val t = Review.open(path).turnAt(turn.toLong())
    PreEndgameAnalyzer(t.analyzePreEndgame(), parseBoard(t.board))
  }

  private fun logFiles(): List<File> =
    dir.listFiles { f -> f.name.endsWith("-log.json") }.orEmpty().sortedByDescending { it.name }

  private fun summary(path: String, g: Game): FinishedGame = FinishedGame(
    path = path, mode = mode(g.mode), startedAt = g.startedAt,
    opponent = g.opponent, myScore = g.myScore.toInt(), opponentScore = g.opponentScore.toInt(),
    outcome = Outcome.of(g.outcome), hits = g.hits.toInt(), bingos = g.bingos.toInt(), bestWord = g.bestWord,
    bestWordScore = g.bestWordScore.toInt(), longestWord = g.longestWord, turns = g.turnCount().toInt(),
    humanStarts = g.humanStarts,
  )

  private fun mode(mode: String): Mode = when (mode) {
    Review.ModeClassic -> Mode.CLASSIC
    Review.ModeEndgame -> Mode.ENDGAME
    else -> Mode.DUPLICATE
  }
}
