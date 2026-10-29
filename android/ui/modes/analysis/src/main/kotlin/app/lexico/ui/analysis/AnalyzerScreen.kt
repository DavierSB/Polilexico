package app.lexico.ui.analysis

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.lexico.ui.board.BlankLetterDialog
import app.lexico.ui.board.BoardStyle
import app.lexico.ui.board.ScrabbleBoard
import app.lexico.ui.common.BarTextButton
import app.lexico.ui.common.Compact
import app.lexico.ui.common.Header
import app.lexico.ui.common.Notice
import app.lexico.ui.common.RankedMoveList
import kotlinx.coroutines.launch

@Composable
fun AnalyzerScreen(
  style: BoardStyle, analyst: Analyst, onBack: () -> Unit, editor: PositionEditor = rememberPositionEditor(), onDeep: (EditedPosition) -> Unit = {},
) {
  val analysis = rememberAnalysis()
  val scope = rememberCoroutineScope()
  var scoring by remember { mutableStateOf(false) }
  LaunchedEffect(editor.version) { analysis.clear() }
  Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
    Header("Analizador", onBack) { BarTextButton("Marcador") { scoring = true }; BarTextButton("Vaciar", editor::clear) }
    AnalysisRack(editor.rack, style, analysis.running, onRemove = editor::removeFromRack) { scope.launch { analysis.run(analyst, editor, onDeep) } }
    analysis.message?.let { Notice(it) }
    AnalysisBoard(editor, analysis, style)
    if (analysis.result != null) ResultsPanel(editor, analysis) else EditPanel(editor, style)
  }
  if (editor.pendingBlank != null) BlankLetterDialog(editor::placeBlank)
  if (scoring) ScoreDialog(editor) { scoring = false }
}

@Composable
private fun AnalysisBoard(editor: PositionEditor, analysis: Analysis, style: BoardStyle) {
  val editing = analysis.result == null
  ScrabbleBoard(
    editor.state.board, Modifier.fillMaxWidth(), style,
    pending = analysis.selected?.placement?.placed.orEmpty(),
    arrow = editor.state.arrow.takeIf { editing && editor.target == Target.BOARD },
  ) { if (editing) editor.tapSquare(it) }
}

@Composable
private fun ColumnScope.ResultsPanel(editor: PositionEditor, analysis: Analysis) {
  Row(verticalAlignment = Alignment.CenterVertically) {
    Text("Mejores jugadas", Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
    analysis.selected?.takeIf { it.placement != null }?.let { c ->
      TextButton(contentPadding = Compact, onClick = { editor.placeCandidate(c) }) { Text("Ponerla") }
    }
    TextButton(contentPadding = Compact, onClick = analysis::clear) { Text("Editar") }
  }
  RankedMoveList(analysis.result.orEmpty(), analysis.selected, Modifier.weight(1f)) { analysis.selected = it }
}

@Composable
private fun ColumnScope.EditPanel(editor: PositionEditor, style: BoardStyle) {
  TargetSelector(editor.target, editor::selectTarget)
  LetterPalette(
    style, marked = editor.letter.takeIf { editor.target == Target.BOARD }, modifier = Modifier.weight(1f), onLetter = editor::tapLetter,
  )
}
