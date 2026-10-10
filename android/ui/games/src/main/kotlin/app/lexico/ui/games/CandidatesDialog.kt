package app.lexico.ui.games

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.lexico.ui.common.RankedMove
import app.lexico.ui.common.RankedMoveList

@Composable
internal fun CandidatesDialog(moves: List<RankedMove>, selected: RankedMove?, close: () -> Unit, select: (RankedMove) -> Unit) {
  Dialog(onDismissRequest = close, properties = DialogProperties(usePlatformDefaultWidth = false)) {
    Surface(Modifier.fillMaxWidth().fillMaxHeight(0.85f).padding(horizontal = 8.dp), shape = AlertDialogDefaults.shape) {
      Column(Modifier.padding(start = 12.dp, end = 12.dp, top = 20.dp, bottom = 8.dp)) {
        Text("Mejores jugadas", Modifier.padding(start = 4.dp, bottom = 12.dp), style = MaterialTheme.typography.headlineSmall)
        RankedMoveList(moves, selected, Modifier.weight(1f), select)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { TextButton(onClick = close) { Text("Cerrar") } }
      }
    }
  }
}
