package app.lexico.ui.duplicate

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import app.lexico.ui.common.FitColumn
import app.lexico.ui.common.Header

@Composable
fun NewOnlineDuplicateScreen(onBack: () -> Unit, onCreate: (turnMs: Long, rounds: Int, live: Boolean) -> Unit) {
  FitColumn(10.dp) {
    Header("Duplicada en línea", onBack)
    DuplicateRoomSettings(enabled = true, onCreate)
  }
}
