package app.lexico.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

data class RulesSection(val text: String, val title: String? = null)

@Composable
fun RulesDialog(title: String, sections: List<RulesSection>, close: () -> Unit) {
  AlertDialog(
    onDismissRequest = close,
    title = { Text(title) },
    text = { RulesBody(sections) },
    confirmButton = { TextButton(onClick = close) { Text("Entendido") } },
  )
}

@Composable
fun RulesButton(onClick: () -> Unit) {
  TextButton(onClick, Modifier.height(36.dp), contentPadding = PaddingValues(horizontal = 4.dp)) {
    Icon(LexicoIcons.Rules, null, Modifier.size(18.dp))
    Text("Leer reglas", Modifier.padding(start = 8.dp))
  }
}

@Composable
fun RulesBarButton(onClick: () -> Unit) = BarIconButton(LexicoIcons.Rules, "Reglas", onClick)

@Composable
private fun RulesBody(sections: List<RulesSection>) {
  Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
    sections.forEach { RulesParagraph(it) }
  }
}

@Composable
private fun RulesParagraph(section: RulesSection) {
  Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
    section.title?.let { Text(it, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary) }
    Text(section.text, style = MaterialTheme.typography.bodyMedium)
  }
}
