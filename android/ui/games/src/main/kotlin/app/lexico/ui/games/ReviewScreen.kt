package app.lexico.ui.games

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.lexico.ui.board.BoardStyle
import app.lexico.ui.common.BagDialog
import app.lexico.ui.common.BagIcon
import app.lexico.ui.common.BarIconButton
import app.lexico.ui.common.BarTextButton
import app.lexico.ui.common.EfficiencyDialog
import app.lexico.ui.common.Header
import app.lexico.ui.common.LexicoIcons
import app.lexico.ui.common.SpreadDialog
import app.lexico.ui.common.ThemeButton

@Composable
fun ReviewScreen(
  review: ReviewView, style: BoardStyle, analyst: EndgameAnalyst?, atEndgame: Boolean, onBack: () -> Unit, onTheme: () -> Unit,
) {
  var page by remember { mutableIntStateOf(firstPage(review, atEndgame)) }
  var dialog by remember { mutableStateOf(if (review.turns.isEmpty() || atEndgame) ReviewDialog.NONE else ReviewDialog.OPENING) }
  val go = { i: Int -> page = i.coerceIn(0, review.turns.lastIndex) }
  val analyses = rememberEndgameAnalyses(analyst)
  Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
    Header(title(review.turns.getOrNull(page)), onBack, titleLines = 2) { ReviewActions(review.turns.getOrNull(page)?.bag, chartName(review), onTheme) { dialog = it } }
    if (review.turns.isEmpty()) return@Column Text("La partida no tiene turnos.")
    BoxWithConstraints(Modifier.fillMaxWidth().weight(1f)) {
      val height = maxHeight
      Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) { TurnPage(review, page, analyses, style, height, go) }
    }
  }
  ReviewDialogShown(dialog, review, page, go) { dialog = ReviewDialog.NONE }
}

private fun firstPage(review: ReviewView, atEndgame: Boolean): Int {
  val endgame = endgameStart(review).takeIf { atEndgame }
  return (endgame ?: review.startTurn).coerceIn(0, maxOf(0, review.turns.lastIndex))
}

private fun endgameStart(review: ReviewView): Int? = review.turns.indexOfFirst { it.endgame }.takeIf { it >= 0 }

private fun title(turn: ReviewTurnView?): String = if (turn?.endgame == true) "Análisis del final" else "Análisis"

private enum class ReviewDialog { NONE, OPENING, ADVANTAGE, MOVES, BAG }

@Composable
private fun ReviewActions(bag: Int?, chart: String, onTheme: () -> Unit, open: (ReviewDialog) -> Unit) {
  BarTextButton("Movidas") { open(ReviewDialog.MOVES) }
  ThemeButton(onTheme)
  BarIconButton(LexicoIcons.Chart, chart) { open(ReviewDialog.ADVANTAGE) }
  bag?.let { BagIcon(it) { open(ReviewDialog.BAG) } }
}

@Composable
private fun ReviewDialogShown(dialog: ReviewDialog, review: ReviewView, page: Int, go: (Int) -> Unit, close: () -> Unit) {
  when (dialog) {
    ReviewDialog.OPENING -> OpeningChart(review, page, go, close)
    ReviewDialog.ADVANTAGE -> ChartShown(review, page, go, close)
    ReviewDialog.MOVES -> ReviewMovesDialog(review.moves, close) { go(it); close() }
    ReviewDialog.BAG -> BagDialog(bagTitle(review, review.turns[page]), review.turns[page].unseen, close)
    ReviewDialog.NONE -> Unit
  }
}

@Composable
private fun ChartShown(review: ReviewView, page: Int, go: (Int) -> Unit, close: () -> Unit) {
  val efficiency = review.efficiency
  if (efficiency != null) EfficiencyDialog(efficiency, close, page, go) else SpreadDialog(review.spread, close, page, go)
}

@Composable
private fun OpeningChart(review: ReviewView, page: Int, go: (Int) -> Unit, close: () -> Unit) {
  val endgame = endgameStart(review)
  if (review.efficiency != null || endgame == null) return ChartShown(review, page, go, close)
  SpreadDialog(review.spread, close, page, go) { ReviewChoices({ go(review.startTurn); close() }, { go(endgame); close() }) }
}

@Composable
private fun ReviewChoices(onGame: () -> Unit, onEndgame: () -> Unit) {
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Button(onClick = onGame, Modifier.fillMaxWidth()) { Text("Analizar partida") }
    Button(onClick = onEndgame, Modifier.fillMaxWidth()) { Text("Analizar final") }
  }
}

private fun chartName(review: ReviewView): String = if (review.efficiency != null) "Eficiencia" else "Ventaja"

private fun bagTitle(review: ReviewView, turn: ReviewTurnView): String =
  if (review.classic) "Por salir (bolsa ${turn.bag} + atril del rival)" else "Quedan en la bolsa"
