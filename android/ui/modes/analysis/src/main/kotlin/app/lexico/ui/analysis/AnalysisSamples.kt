package app.lexico.ui.analysis

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import app.lexico.model.Placement
import app.lexico.ui.common.LexicoTheme
import app.lexico.ui.common.RankedMove
import app.lexico.ui.board.BoardStyles

/** Un analista de ejemplo, para las vistas previas y para probar la pantalla sin motor. */
object AnalysisSamples {
  /** Siempre las mismas tres jugadas, sin mirar la posicion. */
  val fixedAnalyst = Analyst { _, _ ->
    listOf(
      RankedMove("H4 TRECHO", 30, 38.5, Placement.parse("h4 TRECHO")),
      RankedMove("H8 RECHISTE", 24, 31.0, Placement.parse("h8 RE[CH]ISTE")),
      RankedMove("cambiar CH?", 0, 12.2),
    )
  }
}

@Preview(widthDp = 400, heightDp = 860)
@Composable
private fun PreviewAnalyzer() = LexicoTheme {
  AnalyzerScreen(BoardStyles.Night, AnalysisSamples.fixedAnalyst, onBack = {})
}
