package app.lexico.ui.sprint

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import app.lexico.ui.common.OutcomeBanner
import app.lexico.ui.common.ResultButtons
import app.lexico.ui.common.ResultCard
import app.lexico.ui.common.ResultChip
import app.lexico.ui.common.ResultSection
import app.lexico.ui.common.WordRow
import java.util.Locale

@Composable
fun Revealed(result: HandResult, shown: Bingo?, onShow: (Bingo) -> Unit, onNext: () -> Unit) {
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    HandBanner(result)
    NextButton(onNext)
    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) { lastHand(available(result.bingos.size), result.bingos, shown, onShow) }
  }
}

@Composable
fun Finished(result: HandResult?, view: SprintView, shown: Bingo?, onShow: (Bingo) -> Unit, actions: SprintActions) {
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    val why = if (result?.outcome == Outcome.TIMEOUT) "¡TIEMPO!" else "SIN VIDAS"
    ResultCard(why, "${view.solved}", if (view.solved == 1) "scrabble resuelto" else "scrabbles resueltos", recordChips(view.record))
    ResultButtons("Otra serie", actions::restart, "Salir", actions::exit)
    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
      if (view.played.isNotEmpty()) item { ResultSection("Tus scrabbles", "Toca una para ver su ficha") }
      items(view.played) { WordRow(it.word, score = it.score) }
      result?.takeIf { it.bingos.isNotEmpty() }?.let { lastHand("La última mano", it.bingos, shown, onShow) }
    }
  }
}

private fun LazyListScope.lastHand(title: String, bingos: List<Bingo>, shown: Bingo?, onShow: (Bingo) -> Unit) {
  item { ResultSection(title, "Toca para verlo en el tablero") }
  items(bingos) { WordRow(it.word, tag = it.at, score = it.score, selected = it == shown) { onShow(it) } }
}

@Composable
private fun HandBanner(result: HandResult) {
  val answer = result.answer
  when (result.outcome) {
    Outcome.SOLVED -> OutcomeBanner("¡SCRABBLE!", answer?.word?.uppercase(Locale.ROOT).orEmpty(), answer?.let { "· ${it.at} · ${it.score} pts" }.orEmpty(), good = true)
    Outcome.TIMEOUT -> OutcomeBanner("SE ACABÓ EL TIEMPO", "Sin scrabble", good = false)
    Outcome.GAVE_UP -> OutcomeBanner("TE RENDISTE", "Pierdes una vida", good = false)
    Outcome.INVALID -> OutcomeBanner("PALABRA NO VÁLIDA", "Pierdes una vida", good = false)
  }
}

private fun recordChips(record: Record?): List<ResultChip> = when {
  record == null -> emptyList()
  record.isNew -> listOf(ResultChip("¡Nuevo récord!", highlighted = true))
  else -> listOf(ResultChip("Récord: ${record.best}"))
}

private fun available(count: Int): String = if (count == 1) "Había 1 scrabble" else "Había $count scrabbles"
