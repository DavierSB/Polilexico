package app.lexico.ui.duplicate

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import app.lexico.model.Placement
import app.lexico.model.Board
import app.lexico.ui.common.LexicoTheme
import app.lexico.ui.board.BoardStyles

/** Partidas de ejemplo, para las vistas previas y para probar la pantalla sin motor. */
object DuplicateSamples {
  val rack = listOf("E", "CH", "R", "?", "T", "I", "S")

  private val rounds = listOf(
    Round(1, RoundPlay("H4 CASERO", 28), RoundPlay("H8 CASA", 12), hit = false),
    Round(2, RoundPlay("8H CERO", 6), RoundPlay("8H CERO", 6), hit = true),
    Round(3, RoundPlay("K5 LOBO", 9), RoundPlay("pase", 0), hit = false),
  )

  /** Tres rondas cerradas, pensando la cuarta. */
  val playing = DuplicateView(
    board = Board.of("h8 CASA", "8h .ERO", "k5 LOB."),
    phase = Phase.Playing(rack, remainingMs = 143_000),
    rounds = rounds,
    bag = "AAAAAAAAABBCCDDDDEEEEEEEEFGHIIIIJLLLLMMNNNNÑOOOOOPPQRRRSSSSTTTUUUVXYZ?".map { it.toString() },
  )

  val waiting = playing.copy(phase = Phase.Waiting(DEFAULT_TURN_MS))

  val finished = playing.copy(phase = Phase.Finished)

  /** Acciones que no hacen nada, para las vistas previas. */
  val noActions = object : DuplicateActions {
    override fun showRack() {}
    override fun propose(placement: Placement) {}
    override fun pass() {}
    override fun cancel() {}
    override fun resign() {}
    override fun exit() {}
    override fun analyze() {}
    override fun pause() {}
    override fun resume() {}
  }
}

@Preview(widthDp = 400, heightDp = 860)
@Composable
private fun PreviewPlaying() = LexicoTheme {
  DuplicateScreen(DuplicateSamples.playing, BoardStyles.Night, DuplicateSamples.noActions)
}

@Preview(widthDp = 400, heightDp = 860)
@Composable
private fun PreviewWaiting() = LexicoTheme {
  DuplicateScreen(DuplicateSamples.waiting, BoardStyles.Night, DuplicateSamples.noActions)
}
