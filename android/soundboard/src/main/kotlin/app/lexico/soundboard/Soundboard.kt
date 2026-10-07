package app.lexico.soundboard

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun Soundboard() {
  var celebrating by remember { mutableStateOf<Int?>(null) }
  MaterialTheme(if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()) {
    Surface(Modifier.fillMaxSize()) {
      Column(Modifier.safeDrawingPadding().padding(16.dp)) {
        Text("Sonidos de Material", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Toca uno para escucharlo.", Modifier.padding(bottom = 4.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Sounds { celebrating = it }
      }
    }
    celebrating?.let { CelebrationOverlay(it) { celebrating = null } }
  }
}

@Composable
private fun Sounds(celebrate: (Int) -> Unit) {
  val ctx = LocalContext.current
  val play: (Slot, Variant) -> Unit = { slot, v ->
    if (slot == Slot.CELEBRATION) rawSound(ctx, v.file)?.let(celebrate) else play(ctx, v)
  }
  LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
    PENDING.forEach { (slot, variants) -> pendingSection(slot, variants) { play(slot, it) } }
    chosenSection(play)
  }
}

private fun LazyListScope.pendingSection(slot: Slot, variants: List<Variant>, play: (Variant) -> Unit) {
  item { SectionTitle("Por elegir: ${slot.title}", slot.use) }
  itemsIndexed(variants) { i, v -> SoundCard("Variante ${i + 1}" + variantNote(slot, i), v.label) { play(v) } }
}

private fun variantNote(slot: Slot, index: Int): String = if (slot == Slot.MISS && index == 0) " (el acierto, más grave)" else ""

private fun LazyListScope.chosenSection(play: (Slot, Variant) -> Unit) {
  item { SectionTitle("Ya elegidos", "Así sonará Poliléxico.") }
  items(CHOSEN.keys.toList()) { slot -> SoundCard(slot.title, slot.use) { play(slot, CHOSEN.getValue(slot)) } }
}

@Composable
private fun SectionTitle(title: String, detail: String) {
  Column(Modifier.padding(top = 14.dp)) {
    Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
  }
}

@Composable
private fun SoundCard(title: String, detail: String, onClick: () -> Unit) {
  ElevatedCard(onClick = onClick, Modifier.fillMaxWidth()) {
    Column(Modifier.padding(16.dp)) {
      Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
      Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
  }
}
