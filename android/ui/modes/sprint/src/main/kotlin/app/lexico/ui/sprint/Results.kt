package app.lexico.ui.sprint

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun Revealed(result: HandResult, shown: Bingo?, onShow: (Bingo) -> Unit, onNext: () -> Unit) {
  Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
    OutcomeLine(result)
    NextButton(onNext)
    BingoList(result.bingos, shown, onShow)
  }
}

@Composable
fun Finished(result: HandResult?, solved: Int, record: Record?, shown: Bingo?, onShow: (Bingo) -> Unit, actions: SprintActions) {
  Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
    val why = if (result?.outcome == Outcome.TIMEOUT) "¡Tiempo!" else "Sin vidas."
    Text("$why Resolviste ${scrabbles(solved)}.", style = MaterialTheme.typography.titleMedium)
    record?.let { RecordLine(it) }
    result?.takeIf { it.outcome != Outcome.TIMEOUT }?.let { OutcomeLine(it) }
    FinishedButtons(actions::restart, actions::exit)
    result?.let { BingoList(it.bingos, shown, onShow) }
  }
}

@Composable
private fun RecordLine(record: Record) {
  if (record.isNew) Text("¡Nuevo récord!", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
  else Text("Récord: ${scrabbles(record.best)}")
}

@Composable
private fun OutcomeLine(result: HandResult) {
  val count = result.bingos.size
  val available = "Había $count ${if (count == 1) "scrabble" else "scrabbles"}."
  Text("${outcomeText(result)} $available", fontWeight = FontWeight.Bold)
}

@Composable
private fun BingoList(bingos: List<Bingo>, shown: Bingo?, onShow: (Bingo) -> Unit) {
  LazyColumn {
    items(bingos) { BingoRow(it, selected = it == shown) { onShow(it) } }
  }
}

@Composable
private fun BingoRow(bingo: Bingo, selected: Boolean, onClick: () -> Unit) {
  val background = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface
  Row(Modifier.fillMaxWidth().background(background).clickable(onClick = onClick).padding(horizontal = 8.dp, vertical = 4.dp)) {
    Text(bingo.text, Modifier.weight(1f), fontFamily = FontFamily.Monospace)
    Text("${bingo.score}", fontFamily = FontFamily.Monospace)
  }
}

private fun outcomeText(result: HandResult): String = when (result.outcome) {
  Outcome.SOLVED -> "¡Scrabble! ${result.answer?.let { "${it.text} (${it.score})" }.orEmpty()}"
  Outcome.TIMEOUT -> "Se acabó el tiempo."
  Outcome.GAVE_UP -> "Te rendiste: pierdes una vida."
  Outcome.INVALID -> "Palabra no válida: pierdes una vida."
}
