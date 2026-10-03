package app.lexico.ui.classic

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import kotlin.math.roundToInt

@Composable
fun RangeBar(
  min: Int, max: Int, range: IntRange, step: Int, names: Pair<String, String>, show: (Int) -> String, onChange: (min: Int, max: Int) -> Unit,
) {
  Column {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
      RangeLabel(names.first, show(min))
      RangeLabel(names.second, show(max))
    }
    RangeSlider(
      value = min.toFloat()..max.toFloat(),
      onValueChange = { onChange(snap(it.start, step), snap(it.endInclusive, step)) },
      valueRange = range.first.toFloat()..range.last.toFloat(),
      steps = (range.last - range.first) / step - 1,
      colors = rangeColors(),
    )
    Scale(range, show)
  }
}

@Composable
private fun RangeLabel(name: String, value: String) {
  Text("$name: $value", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
}

@Composable
private fun Scale(range: IntRange, show: (Int) -> String) {
  Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
    listOf(range.first, (range.first + range.last) / 2, range.last).forEach { Text(show(it), style = MaterialTheme.typography.labelSmall) }
  }
}

@Composable
private fun rangeColors() = SliderDefaults.colors(
  thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary, activeTickColor = Color.Transparent,
  inactiveTickColor = Color.Transparent,
)

private fun snap(x: Float, step: Int): Int = (x / step).roundToInt() * step
