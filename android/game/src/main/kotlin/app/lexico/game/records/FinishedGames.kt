package app.lexico.game.records

import app.lexico.game.Outcome
import app.lexico.game.engine.engine
import app.lexico.game.engine.parseBoard
import app.lexico.game.storage.Mode
import app.lexico.go.review.Game
import app.lexico.go.review.Review
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val WATCHED = "watched-"
private const val STAMP = "yyyyMMdd-HHmmss"

class FinishedGames internal constructor(private val dir: File, private val online: OnlineRecords? = null) {
  suspend fun list(): List<FinishedGame> = engine {
    logFiles().mapNotNull { f -> runCatching { summary(f.path, Review.open(f.path)).copy(finishedAt = f.lastModified()) }.getOrNull() }
  }

  suspend fun open(path: String): GameReview = engine {
    val game = Review.open(path)
    GameReview(summary(path, game), (0 until game.turnCount()).map { reviewTurn(game.turnAt(it)) }, game.startTurn.toInt())
  }

  suspend fun ranked(path: String, turn: Int): ReviewTurn = engine {
    reviewTurn(Review.open(path).turnAt(turn.toLong()).rank())
  }

  suspend fun explore(path: String, turn: Int): EndgameExplorer = engine {
    val t = Review.open(path).turnAt(turn.toLong())
    EndgameExplorer(t.explore(), parseBoard(t.board))
  }

  suspend fun preEndgame(path: String, turn: Int): PreEndgameAnalyzer = engine {
    val t = Review.open(path).turnAt(turn.toLong())
    PreEndgameAnalyzer(t.analyzePreEndgame(), parseBoard(t.board))
  }

  fun keep(log: String, gcg: String, watched: Boolean): String {
    dir.mkdirs()
    val base = File(dir, (if (watched) WATCHED else "") + "duplicate-" + SimpleDateFormat(STAMP, Locale.US).format(Date())).path
    File("$base.gcg").writeText(gcg)
    return File("$base-log.json").apply { writeText(log) }.path
  }

  private fun logFiles(): List<File> =
    dir.listFiles { f -> f.name.endsWith("-log.json") && !f.name.startsWith(WATCHED) }.orEmpty().sortedByDescending { it.name }

  private fun summary(path: String, g: Game): FinishedGame = FinishedGame(
    path = path, mode = mode(g.mode), startedAt = g.startedAt,
    opponent = g.opponent, myScore = g.myScore.toInt(), opponentScore = g.opponentScore.toInt(),
    outcome = Outcome.of(g.outcome), hits = g.hits.toInt(), bingos = g.bingos.toInt(), bestWord = g.bestWord,
    bestWordScore = g.bestWordScore.toInt(), longestWord = g.longestWord, turns = g.turnCount().toInt(),
    humanStarts = g.humanStarts, host = g.me.takeIf { g.local },
    me = RecordSeat(g.me, g.meCountry, g.meID), rival = RecordSeat(g.opponent, g.opponentCountry, g.opponentID),
    watched = File(path).name.startsWith(WATCHED), online = online?.contains(path) == true || g.opponentID.isNotEmpty(),
  )

  private fun mode(mode: String): Mode = when (mode) {
    Review.ModeClassic -> Mode.CLASSIC
    Review.ModeEndgame -> Mode.ENDGAME
    else -> Mode.DUPLICATE
  }
}
