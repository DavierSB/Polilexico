package app.lexico.ui.classic

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import kotlin.math.roundToInt

/** De 5 en 5 puntos. */
private const val LEAD_STEP = 5

/**
 * Una ventaja minima y una maxima sobre una barra de -limit a limit: un puntero para cada una y,
 * entre ellos, el tramo elegido del color de la app.
 */
@Composable
fun LeadRangeBar(min: Int, max: Int, limit: Int, onChange: (min: Int, max: Int) -> Unit) {
  Column {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
      LeadLabel("Mínima", min)
      LeadLabel("Máxima", max)
    }
    RangeSlider(
      value = min.toFloat()..max.toFloat(),
      onValueChange = { onChange(snap(it.start), snap(it.endInclusive)) },
      valueRange = -limit.toFloat()..limit.toFloat(),
      steps = 2 * limit / LEAD_STEP - 1,
      colors = rangeColors(),
    )
    Scale(limit)
  }
}

@Composable
private fun LeadLabel(name: String, value: Int) {
  Text("$name: ${signed(value)}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
}

/** -limit, 0 y limit bajo la barra. */
@Composable
private fun Scale(limit: Int) {
  Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
    listOf(-limit, 0, limit).forEach { Text(signed(it), style = MaterialTheme.typography.labelSmall) }
  }
}

@Composable
private fun rangeColors() = SliderDefaults.colors(
  thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary, activeTickColor = Color.Transparent,
  inactiveTickColor = Color.Transparent,
)

private fun snap(x: Float): Int = (x / LEAD_STEP).roundToInt() * LEAD_STEP

/** "+20", "0", "-40". */
private fun signed(n: Int): String = if (n > 0) "+$n" else "$n"
