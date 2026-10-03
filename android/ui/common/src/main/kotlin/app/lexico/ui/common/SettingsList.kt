package app.lexico.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class SettingItem(val text: String, val highlight: String? = null, val dialog: @Composable (close: () -> Unit) -> Unit)

@Composable
fun SettingsList(items: List<SettingItem>) {
  var open by remember { mutableStateOf<Int?>(null) }
  Column {
    SettingsHeader()
    Column(Modifier.padding(top = 4.dp)) { items.forEachIndexed { i, item -> SettingRow(item) { open = i } } }
  }
  open?.let { items.getOrNull(it)?.dialog?.invoke { open = null } }
}

@Composable
fun SettingDialog(
  title: String, explanation: String, close: () -> Unit, onAccept: (() -> Unit)?, content: @Composable ColumnScope.() -> Unit,
) {
  AlertDialog(
    onDismissRequest = close,
    title = { Text(title) },
    text = { SettingDialogBody(explanation, content) },
    confirmButton = { TextButton(onClick = { onAccept?.invoke(); close() }, enabled = onAccept != null) { Text("Aceptar") } },
    dismissButton = { TextButton(onClick = close) { Text("Cancelar") } },
  )
}

@Composable
fun RadioOption(text: String, selected: Boolean, onSelect: () -> Unit) {
  Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable(onClick = onSelect), verticalAlignment = Alignment.CenterVertically) {
    RadioButton(selected = selected, onClick = onSelect)
    Text(text)
  }
}

@Composable
private fun SettingsHeader() {
  Text("Ajustes", style = TextStyle(fontFamily = Mulish, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface))
  Text("Toca un ajuste para cambiarlo", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun SettingRow(item: SettingItem, onClick: () -> Unit) {
  Row(
    Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(horizontal = 6.dp, vertical = 9.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Box(Modifier.size(8.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
    Text(styled(item), Modifier.padding(start = 14.dp), style = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp))
  }
}

@Composable
private fun styled(item: SettingItem): AnnotatedString {
  val at = item.highlight?.let { item.text.indexOf(it) } ?: -1
  if (at < 0) return AnnotatedString(item.text)
  val strong = SpanStyle(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
  return buildAnnotatedString {
    append(item.text.substring(0, at))
    withStyle(strong) { append(item.highlight!!) }
    append(item.text.substring(at + item.highlight!!.length))
  }
}

@Composable
private fun SettingDialogBody(explanation: String, content: @Composable ColumnScope.() -> Unit) {
  Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Text(explanation)
    content()
  }
}
