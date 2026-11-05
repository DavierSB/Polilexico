package app.lexico.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp

@Composable
fun WatchersButton(watchers: List<PersonItem>) {
  if (watchers.isEmpty()) return
  var open by remember { mutableStateOf(false) }
  BarTextButton(watchersLabel(watchers.size)) { open = true }
  if (open) WatchersDialog(watchers) { open = false }
}

fun watchersLabel(count: Int): String = "👁 $count"

@Composable
private fun WatchersDialog(watchers: List<PersonItem>, close: () -> Unit) {
  AlertDialog(
    onDismissRequest = close, title = { Text("Observando") },
    text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { watchers.forEach { PlayerRow(it.name, it.country) {} } } },
    confirmButton = { TextButton(onClick = close) { Text("Cerrar") } },
  )
}
