package app.lexico.ui.lists

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.lexico.ui.common.Compact

@Composable
internal fun LabeledStepper(label: String, value: String, modifier: Modifier, onMinus: (() -> Unit)?, onPlus: (() -> Unit)?) {
  Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
    Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
      OutlinedButton(onClick = { onMinus?.invoke() }, enabled = onMinus != null, contentPadding = Compact) { Text("−") }
      Text(value, Modifier.width(32.dp), fontSize = 20.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
      OutlinedButton(onClick = { onPlus?.invoke() }, enabled = onPlus != null, contentPadding = Compact) { Text("+") }
    }
  }
}

@Composable
internal fun MiniTile(tile: Char) {
  Text(
    WordKeys.label(tile),
    Modifier.clip(RoundedCornerShape(4.dp)).background(MaterialTheme.colorScheme.primaryContainer).padding(horizontal = 6.dp, vertical = 2.dp),
    color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold, fontSize = 14.sp,
  )
}

@Composable
internal fun SlotRow(n: Int, slots: String, fromEnd: Boolean, onSlots: (String) -> Unit) {
  BoxWithConstraints {
    val side = minOf(46.dp, (maxWidth - 3.dp * (n - 1)) / n)
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
      for (i in if (fromEnd) (n - 1 downTo 0) else (0 until n)) {
        SlotTile(i, slots.getOrElse(i) { FilterSpec.FREE }, Modifier.width(side)) { onSlots(replaced(slots, i, it)) }
      }
    }
  }
}

private fun replaced(slots: String, i: Int, tile: Char): String =
  slots.padEnd(MAX_TILES, FilterSpec.FREE).replaceRange(i, i + 1, tile.toString())

@Composable
private fun SlotTile(index: Int, tile: Char, modifier: Modifier, onChange: (Char) -> Unit) {
  val filled = tile != FilterSpec.FREE
  val colors = MaterialTheme.colorScheme
  BasicTextField(
    value = if (filled) WordKeys.label(tile) else "",
    onValueChange = { text -> onChange(WordKeys.parse(text).lastOrNull() ?: FilterSpec.FREE) },
    singleLine = true,
    textStyle = TextStyle(color = colors.onPrimaryContainer, fontWeight = FontWeight.Bold, fontSize = 15.sp, textAlign = TextAlign.Center),
    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
    modifier = modifier.aspectRatio(.85f).clip(RoundedCornerShape(5.dp))
      .background(if (filled) colors.primaryContainer else colors.primary.copy(alpha = .1f)),
    decorationBox = { inner -> SlotDecoration(index, filled, inner) },
  )
}

@Composable
private fun SlotDecoration(index: Int, filled: Boolean, inner: @Composable () -> Unit) {
  Box(Modifier.fillMaxSize(), Alignment.Center) {
    if (!filled) Text("${index + 1}", color = MaterialTheme.colorScheme.primary.copy(alpha = .4f), fontSize = 11.sp)
    inner()
  }
}
