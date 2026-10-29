package app.lexico.ui.lists

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
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

class ListsPage(val title: String, val entries: List<CatalogEntry>, val mine: Boolean = false, val emptyNote: String = "Pronto habrá listas en esta carpeta.")

@Composable
fun ListsScreen(page: ListsPage, onBack: () -> Unit, onCreate: () -> Unit, onFolder: (Folder) -> Unit, onOpen: (WordList) -> Unit) {
  Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
    Header(page.title, onBack)
    when {
      page.entries.isNotEmpty() -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) { pageItems(page, onFolder, onOpen) }
      page.mine -> NoLists(onCreate)
      else -> Soon(page.emptyNote)
    }
  }
}

private fun LazyListScope.pageItems(page: ListsPage, onFolder: (Folder) -> Unit, onOpen: (WordList) -> Unit) {
  items(page.entries, key = ::entryKey) { entry ->
    when (entry) {
      is CatalogEntry.Sub -> FolderRow(entry.folder) { onFolder(entry.folder) }
      is CatalogEntry.Leaf -> ListRow(entry.list) { onOpen(entry.list) }
    }
  }
}

private fun entryKey(entry: CatalogEntry): String = when (entry) {
  is CatalogEntry.Sub -> "f:" + entry.folder.path.joinToString("/")
  is CatalogEntry.Leaf -> "l:" + entry.list.id
}

@Composable
private fun Soon(note: String) {
  Column(Modifier.fillMaxSize(), Arrangement.spacedBy(10.dp, Alignment.CenterVertically), Alignment.CenterHorizontally) {
    Title("Todavía no hay listas")
    Text(note, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
  }
}

@Composable
private fun NoLists(onCreate: () -> Unit) {
  Column(Modifier.fillMaxSize(), Arrangement.spacedBy(12.dp, Alignment.CenterVertically), Alignment.CenterHorizontally) {
    Title("No hay listas")
    Text("Crea una para que la puedas estudiar.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant,
      textAlign = TextAlign.Center)
    Button(onClick = onCreate) { Text("Crear lista") }
  }
}

@Composable
private fun FolderRow(folder: Folder, onClick: () -> Unit) {
  EntryCard(folder.name, listOf(count(folder.families)), folder.percent, onClick)
}

@Composable
private fun ListRow(list: WordList, onClick: () -> Unit) {
  EntryCard(list.name, listOf(count(list.members.size)), list.percent, onClick)
}

@Composable
private fun EntryCard(name: String, chips: List<String>, percent: Int, onClick: () -> Unit) {
  Surface(Modifier.fillMaxWidth().clickable(onClick = onClick), RoundedCornerShape(14.dp), MaterialTheme.colorScheme.surfaceContainerHigh) {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Title(name, size = 17.sp)
          Chips(*chips.toTypedArray())
        }
        Text("$percent%", fontFamily = Mulish, fontWeight = FontWeight.Black, fontSize = 24.sp, color = MaterialTheme.colorScheme.primary)
      }
      LinearProgressIndicator(progress = { percent / 100f }, Modifier.fillMaxWidth())
    }
  }
}

private fun count(families: Int): String = if (families == 1) "1 familia" else "$families familias"
