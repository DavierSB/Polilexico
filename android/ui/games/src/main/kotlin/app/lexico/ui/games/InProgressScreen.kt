package app.lexico.ui.games

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import app.lexico.ui.common.ConfirmDialog
import app.lexico.ui.common.Header

data class InProgressItem(val id: String, val title: String, val detail: String)

@Composable
fun InProgressScreen(items: List<InProgressItem>, onBack: () -> Unit, onOpen: (String) -> Unit, onDelete: (String) -> Unit) {
  var toDelete by remember { mutableStateOf<InProgressItem?>(null) }
  Column(Modifier.fillMaxSize()) {
    Header("Partidas en curso", onBack)
    if (items.isEmpty()) EmptyList("No hay partidas sin terminar.")
    LazyColumn {
      items(items, key = { it.id }) { item -> InProgressRow(item, { onOpen(item.id) }) { toDelete = item } }
    }
  }
  toDelete?.let { item -> DeleteDialog(item, onDelete = { onDelete(item.id); toDelete = null }, onKeep = { toDelete = null }) }
}

@Composable
private fun InProgressRow(item: InProgressItem, onOpen: () -> Unit, onDelete: () -> Unit) {
  GameRow(item.title, item.detail, onOpen) { TextButton(onClick = onDelete) { Text("Borrar") } }
}

@Composable
private fun DeleteDialog(item: InProgressItem, onDelete: () -> Unit, onKeep: () -> Unit) {
  ConfirmDialog("¿Borrar «${item.title}»? No se podrá continuar.", yes = "Borrar", no = "No", onYes = onDelete, onNo = onKeep)
}
