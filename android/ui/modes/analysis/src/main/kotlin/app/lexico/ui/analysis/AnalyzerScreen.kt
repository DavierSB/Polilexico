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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.lexico.ui.board.BlankLetterDialog
import app.lexico.ui.board.BoardStyle
import app.lexico.ui.board.ScrabbleBoard
import app.lexico.ui.common.Compact
import app.lexico.ui.common.Header
import app.lexico.ui.common.Notice
import app.lexico.ui.common.RankedMoveList
import kotlinx.coroutines.launch

/**
 * El analizador: un tablero y un atril cualesquiera, armados con la paleta de letras, y las
 * mejores jugadas que da el [analyst] para esa posicion. Mientras hay resultados, la paleta
 * deja su sitio a la lista; tocar una jugada la dibuja en el tablero.
 */
@Composable
fun AnalyzerScreen(style: BoardStyle, analyst: Analyst, onBack: () -> Unit) {
  val editor = rememberPositionEditor()
  val analysis = rememberAnalysis()
  val scope = rememberCoroutineScope()
  // Cualquier cambio en la posicion invalida el analisis anterior.
  LaunchedEffect(editor.version) { analysis.clear() }
  Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
    Header("Analizador", onBack) { TextButton(contentPadding = Compact, onClick = editor::clear) { Text("Vaciar") } }
    AnalysisRack(editor.rack, style, analysis.running, onRemove = editor::removeFromRack) { scope.launch { analysis.run(analyst, editor) } }
    analysis.message?.let { Notice(it) }
    AnalysisBoard(editor, analysis, style)
    if (analysis.result != null) ResultsPanel(editor, analysis) else EditPanel(editor, style)
  }
  if (editor.pendingBlank != null) BlankLetterDialog(editor::placeBlank)
}

/** La posicion con la jugada elegida encima; la flecha solo mientras se edita el tablero. */
@Composable
private fun AnalysisBoard(editor: PositionEditor, analysis: Analysis, style: BoardStyle) {
  val editing = analysis.result == null
  ScrabbleBoard(
    editor.state.board, Modifier.fillMaxWidth(), style,
    pending = analysis.selected?.placement?.placed.orEmpty(),
    arrow = editor.state.arrow.takeIf { editing && editor.target == Target.BOARD },
  ) { if (editing) editor.tapSquare(it) }
}

/** Las mejores jugadas, con los botones para ponerla en el tablero o volver a editar. */
@Composable
private fun ColumnScope.ResultsPanel(editor: PositionEditor, analysis: Analysis) {
  Row(verticalAlignment = Alignment.CenterVertically) {
    Text("Mejores jugadas (valoración de Woogles):", Modifier.weight(1f), style = MaterialTheme.typography.labelMedium)
    // Poner la elegida en el tablero ayuda a montar posiciones jugada a jugada.
    analysis.selected?.takeIf { it.placement != null }?.let { c ->
      TextButton(contentPadding = Compact, onClick = { editor.placeCandidate(c) }) { Text("Ponerla") }
    }
    TextButton(contentPadding = Compact, onClick = analysis::clear) { Text("Editar") }
  }
  RankedMoveList(analysis.result.orEmpty(), analysis.selected, Modifier.weight(1f)) { analysis.selected = it }
}

/** Donde van las letras y la paleta para ponerlas. */
@Composable
private fun ColumnScope.EditPanel(editor: PositionEditor, style: BoardStyle) {
  TargetSelector(editor.target, editor::selectTarget)
  LetterPalette(
    style, marked = editor.letter.takeIf { editor.target == Target.BOARD }, modifier = Modifier.weight(1f), onLetter = editor::tapLetter,
  )
}
