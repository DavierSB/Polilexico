package app.lexico.ui.lists

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.lexico.ui.common.Header

@Composable
fun StudyScreen(list: WordList, lexicon: Lexicon, onBack: () -> Unit, onPosition: (Int) -> Unit, onOpen: (String) -> Unit) {
  var at by remember { mutableIntStateOf(list.studied.coerceIn(0, maxOf(0, list.members.lastIndex))) }
  val move = { to: Int -> at = Math.floorMod(to, list.members.size).also(onPosition) }
  Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
    Header(list.name, onBack)
    Label("${at + 1} de ${list.members.size}")
    val unit = lexicon.find(list.members[at])
    if (unit == null) Text("Esta entrada ya no está en el léxico.", Modifier.weight(1f))
    else key(unit.key) { WordCard(unit, lexicon, onOpen) }
    StudyButtons({ move(at - 1) }, { move(at + 1) })
  }
}

@Composable
private fun StudyButtons(onPrevious: () -> Unit, onNext: () -> Unit) {
  Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), Arrangement.spacedBy(8.dp)) {
    OutlinedButton(onClick = onPrevious, Modifier.weight(1f)) { Text("Anterior") }
    Button(onClick = onNext, Modifier.weight(2f)) { Text("Continuar") }
  }
}
