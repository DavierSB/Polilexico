package app.lexico.ui.common

import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp

private const val NAME_SAMPLE = "Abcdefghijklmno"

@Composable
fun nameWidth(style: TextStyle = LocalTextStyle.current.copy(fontWeight = FontWeight.Bold)): Dp {
  val measurer = rememberTextMeasurer()
  val density = LocalDensity.current
  return remember(style, density) { with(density) { measurer.measure(NAME_SAMPLE, style).size.width.toDp() } }
}
