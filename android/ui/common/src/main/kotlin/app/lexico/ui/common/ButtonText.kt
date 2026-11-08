package app.lexico.ui.common

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp

val RowButtonPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)

@Composable
fun ButtonText(text: String, modifier: Modifier = Modifier, fontSize: TextUnit = TextUnit.Unspecified, fontWeight: FontWeight? = null) {
  val max = fontSize.takeIf { it.isSpecified } ?: LocalTextStyle.current.fontSize.takeIf { it.isSpecified } ?: 14.sp
  Text(text, modifier, fontWeight = fontWeight, maxLines = 1, softWrap = false, autoSize = fitting(max))
}

private fun fitting(max: TextUnit) = TextAutoSize.StepBased(minFontSize = 9.sp, maxFontSize = max, stepSize = 0.5.sp)
