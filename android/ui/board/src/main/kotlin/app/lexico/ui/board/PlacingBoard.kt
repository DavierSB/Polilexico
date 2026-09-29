package app.lexico.ui.board

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * El tablero del [TilePlacer], con tus fichas provisionales y la flecha. Si hace falta, pregunta
 * la letra de un comodin. Con `enabled = false` (no es tu turno) se ve pero no se toca. Sus fichas
 * provisionales se pueden arrastrar (la que se arrastra no se dibuja en su casilla).
 * `latestScore` y `showScore`: los puntos de la ultima jugada, como en [ScrabbleBoard]. Si hay
 * [LocalPlayScorer], tambien los puntos de lo que vas colocando (salvo mientras arrastras).
 */
@Composable
fun PlacingBoard(
  c: TilePlacer,
  style: BoardStyle,
  enabled: Boolean,
  modifier: Modifier = Modifier,
  latestScore: Int? = null,
  showScore: Boolean = false,
) {
  val pending = c.drag.from?.let { c.state.pending - it } ?: c.state.pending
  val arrow = if (enabled) c.state.arrow else null
  val live = rememberLiveScore(c.state.board, c.state.pending).takeIf { enabled && c.drag.tile == null }
  ScrabbleBoard(
    c.state.board, modifier.fillMaxWidth().boardDrags(c, style, enabled), style, pending, arrow, latestScore, showScore,
    liveScore = live,
  ) {
    if (enabled) c.tapBoard(it)
  }
  if (c.pendingBlank != null) BlankLetterDialog(c::placeBlank)
}
