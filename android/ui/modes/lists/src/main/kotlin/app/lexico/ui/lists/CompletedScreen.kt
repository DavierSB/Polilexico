package app.lexico.ui.lists

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.lexico.ui.common.Header
import app.lexico.ui.common.Mulish

class CompletedStudies(lists: List<WordList>) {
  val finished: List<WordList> = lists.filter { it.completions > 0 }.sortedByDescending { it.completions }

  val studies: Int = finished.sumOf { it.completions }

  val families: Int = (lists.flatMap { it.learned } + finished.flatMap { it.members }).toSet().size
}

@Composable
fun CompletedScreen(studies: CompletedStudies, onBack: () -> Unit, onOpen: (WordList) -> Unit) {
  Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
    Header("Estudios culminados", onBack)
    Totals(studies)
    if (studies.finished.isEmpty()) NoneFinished()
    else LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      items(studies.finished, key = { it.id }) { FinishedRow(it) { onOpen(it) } }
    }
  }
}

@Composable
private fun Totals(studies: CompletedStudies) {
  Surface(Modifier.fillMaxWidth(), RoundedCornerShape(14.dp), MaterialTheme.colorScheme.surfaceContainerHigh) {
    Row(Modifier.padding(vertical = 14.dp)) {
      Total(studies.families, if (studies.families == 1) "familia aprendida" else "familias aprendidas")
      Total(studies.studies, if (studies.studies == 1) "estudio culminado" else "estudios culminados")
    }
  }
}

@Composable
private fun RowScope.Total(count: Int, label: String) {
  Column(Modifier.weight(1f), Arrangement.spacedBy(2.dp), Alignment.CenterHorizontally) {
    Big("$count")
    Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
  }
}

@Composable
private fun NoneFinished() {
  Column(Modifier.fillMaxSize(), Arrangement.spacedBy(10.dp, Alignment.CenterVertically), Alignment.CenterHorizontally) {
    Title("Ninguno todavía")
    Text("Una lista queda culminada al aprender todas sus familias en Evaluar.", textAlign = TextAlign.Center,
      color = MaterialTheme.colorScheme.onSurfaceVariant)
  }
}

@Composable
private fun FinishedRow(list: WordList, onClick: () -> Unit) {
  Surface(Modifier.fillMaxWidth().clickable(onClick = onClick), RoundedCornerShape(14.dp), MaterialTheme.colorScheme.surfaceContainerHigh) {
    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
      Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Title(list.name, size = 17.sp)
        Chips(timesText(list.completions))
      }
      Text("×${list.completions}", fontFamily = Mulish, fontWeight = FontWeight.Black, fontSize = 24.sp, color = MaterialTheme.colorScheme.primary)
    }
  }
}

private fun timesText(times: Int): String = if (times == 1) "Culminada 1 vez" else "Culminada $times veces"
