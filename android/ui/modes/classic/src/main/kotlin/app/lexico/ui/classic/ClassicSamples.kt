package app.lexico.ui.classic

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import app.lexico.model.Placement
import app.lexico.model.Board
import app.lexico.ui.common.LexicoTheme
import app.lexico.ui.board.BoardStyles

object ClassicSamples {
  val inProgress = ClassicView(
    opponent = "HastyBot",
    board = Board.of("h8 CASA", "8h .ERO", "k5 LOB.", "5k .UNA", "11h .ÑO"),
    rack = listOf("E", "CH", "R", "?", "T", "I", "S"),
    opponentRack = OpponentRack.Hidden(7),
    myScore = 31,
    opponentScore = 14,
    turn = Side.ME,
    myClock = Clock(remainingMs = 17 * 60_000L + 12_000, overtimeMs = 60_000),
    opponentClock = Clock(remainingMs = 18 * 60_000L + 40_000, overtimeMs = 60_000),
    bag = 71,
    unseen = "AAAAAAAAABBCCDDDDEEEEEEEEFGHIIIIJLLLLMMNNNNÑOOOOOPPQRRRSSSSTTTUUUVXYZ??".map { it.toString() } + "LL" + "RR",
    moves = listOf(
      Move(Side.ME, MoveType.PLACEMENT, "H8 CASA", points = 12, myTotal = 12, opponentTotal = 0),
      Move(Side.OPPONENT, MoveType.PLACEMENT, "8H CERO", points = 6, myTotal = 12, opponentTotal = 6),
      Move(Side.ME, MoveType.PLACEMENT, "K5 LOBO", points = 9, myTotal = 21, opponentTotal = 6),
      Move(Side.OPPONENT, MoveType.PLACEMENT, "5K LUNA", points = 8, myTotal = 21, opponentTotal = 14),
      Move(Side.ME, MoveType.PLACEMENT, "11H AÑO", points = 10, myTotal = 31, opponentTotal = 14),
      Move(Side.OPPONENT, MoveType.EXCHANGE, tiles = 3, myTotal = 31, opponentTotal = 14),
    ),
  )

  val finished = inProgress.copy(
    turn = null,
    opponentRack = OpponentRack.Visible(listOf("Q", "U")),
    end = GameEnd(winner = Side.ME),
  )

  val noActions = object : ClassicActions {
    override fun play(placement: Placement) {}
    override fun exchange(tiles: List<String>) {}
    override fun pass() {}
    override fun resign() {}
    override fun exit() {}
    override fun analyze() {}
    override fun pause() {}
    override fun resume() {}
  }
}

@Preview(widthDp = 400, heightDp = 860)
@Composable
private fun PreviewInProgress() = LexicoTheme {
  ClassicScreen(ClassicSamples.inProgress, BoardStyles.Night, ClassicSamples.noActions)
}

@Preview(widthDp = 400, heightDp = 860)
@Composable
private fun PreviewFinished() = LexicoTheme {
  ClassicScreen(ClassicSamples.finished, BoardStyles.Night, ClassicSamples.noActions)
}
