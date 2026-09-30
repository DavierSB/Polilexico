package app.lexico.game.records

import app.lexico.game.Outcome
import app.lexico.game.engine.engine
import app.lexico.game.engine.parseBoard
import app.lexico.game.engine.moveText
import app.lexico.game.engine.rackTiles
import app.lexico.game.storage.Mode
import app.lexico.go.review.Game
import app.lexico.go.review.Mark
import app.lexico.go.review.Move
import app.lexico.go.review.Review
import app.lexico.go.review.Turn
import app.lexico.model.Board
import app.lexico.model.Placement
import java.io.File

/** Las partidas terminadas: los registros que escribe el motor en `dir` al acabar cada una. */
class FinishedGames internal constructor(private val dir: File) {
  /** Todas, la mas reciente primero. Los registros que no se puedan leer se saltan. */
  suspend fun list(): List<FinishedGame> = engine {
    logFiles().mapNotNull { f -> runCatching { summary(f.path, Review.open(f.path)).copy(finishedAt = f.lastModified()) }.getOrNull() }
  }

  /** La partida del registro `path`, turno a turno. */
  suspend fun open(path: String): GameReview = engine {
    val game = Review.open(path)
    GameReview(summary(path, game), (0 until game.turnCount()).map { turn(game.turnAt(it)) }, game.startTurn.toInt())
  }

  /** Los -log.json; su nombre lleva la fecha, asi que el orden inverso es el mas reciente primero. */
  private fun logFiles(): List<File> =
    dir.listFiles { f -> f.name.endsWith("-log.json") }.orEmpty().sortedByDescending { it.name }

  private fun summary(path: String, g: Game): FinishedGame = FinishedGame(
    path = path, mode = mode(g.mode), startedAt = g.startedAt,
    opponent = g.opponent, myScore = g.myScore.toInt(), opponentScore = g.opponentScore.toInt(),
    outcome = Outcome.of(g.outcome), hits = g.hits.toInt(), bingos = g.bingos.toInt(), bestWord = g.bestWord,
    bestWordScore = g.bestWordScore.toInt(), longestWord = g.longestWord, turns = g.turnCount().toInt(),
  )

  private fun mode(mode: String): Mode = when (mode) {
    Review.ModeClassic -> Mode.CLASSIC
    Review.ModeEndgame -> Mode.ENDGAME
    else -> Mode.DUPLICATE
  }

  private fun turn(t: Turn): ReviewTurn {
    val board = parseBoard(t.board)
    return ReviewTurn(
      number = t.number.toInt(), player = t.player, rack = rackTiles(t.rack), board = board,
      candidates = (0 until t.candidateCount()).map { move(board, t.candidateAt(it)) },
      marks = (0 until t.markCount()).map { mark(board, t.markAt(it)) },
    )
  }

  private fun move(board: Board, m: Move): ReviewMove =
    reviewMove(board, m.description, m.score.toInt(), m.equity.takeIf { m.hasEquity })

  private fun mark(board: Board, m: Mark): ReviewMark =
    ReviewMark(m.who, reviewMove(board, m.description, m.score.toInt(), null), m.rank.toInt().takeIf { it >= 0 })

  /** "H8 CA[CH]A (12 pts)" -> "H8 CACHA", con su colocacion para dibujarla. */
  private fun reviewMove(board: Board, description: String, score: Int, equity: Double?): ReviewMove =
    ReviewMove(moveText(board, description), score, equity, Placement.parseOrNull(description))
}
