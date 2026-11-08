package app.lexico.ui.lists

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.lexico.ui.common.Header
import app.lexico.ui.common.ButtonText
import app.lexico.ui.common.RowButtonPadding
import kotlinx.coroutines.delay

@Composable
fun StudyScreen(list: WordList, lexicon: Lexicon, onBack: () -> Unit, onPosition: (Int) -> Unit, onOpen: (String) -> Unit) {
  var at by remember { mutableIntStateOf(list.studied.coerceIn(0, maxOf(0, list.members.lastIndex))) }
  val move = { to: Int -> at = Math.floorMod(to, list.members.size).also(onPosition) }
  val player = rememberPlayer(LocalContext.current)
  Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
    Header(list.name, onBack)
    PlayerLine(at, list.members.size, player) { if (at < list.members.lastIndex) move(at + 1) else player.playing = false }
    val unit = lexicon.find(list.members[at])
    if (unit == null) Text("Esta entrada ya no está en el léxico.", Modifier.weight(1f))
    else key(unit.key) { WordCard(unit, lexicon, onOpen) }
    StudyButtons(player, { move(at - 1) }, { move(at + 1) })
  }
}

@Composable
private fun PlayerLine(at: Int, size: Int, player: StudyPlayer, onTick: () -> Unit) {
  LaunchedEffect(at, player.playing, player.seconds) {
    if (!player.playing) return@LaunchedEffect
    delay(player.seconds * 1000L)
    onTick()
  }
  Row(verticalAlignment = Alignment.CenterVertically) {
    Label("${at + 1} de $size", Modifier.weight(1f))
    TextButton(onClick = player::nextPace) { Text("${player.seconds} s") }
  }
}

@Composable
private fun StudyButtons(player: StudyPlayer, onPrevious: () -> Unit, onNext: () -> Unit) {
  Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), Arrangement.spacedBy(8.dp)) {
    OutlinedButton(onClick = onPrevious, Modifier.weight(1f), contentPadding = RowButtonPadding) { ButtonText("Anterior") }
    FilledTonalButton(onClick = { player.playing = !player.playing }, Modifier.weight(1.2f), contentPadding = RowButtonPadding) {
      ButtonText(if (player.playing) "Pausar" else "▶ Reproducir")
    }
    Button(onClick = onNext, Modifier.weight(1f), contentPadding = RowButtonPadding) { ButtonText("Siguiente") }
  }
}
