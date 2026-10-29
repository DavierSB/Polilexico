package app.lexico.ui.lists

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.lexico.ui.common.Mulish

@Composable
internal fun Label(text: String, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.primary) {
  Text(text.uppercase(), modifier, color = color, fontFamily = Mulish, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, letterSpacing = 2.sp)
}

@Composable
internal fun Title(text: String, modifier: Modifier = Modifier, size: TextUnit = 22.sp) {
  Text(text, modifier, fontFamily = Mulish, fontWeight = FontWeight.ExtraBold, fontSize = size, color = MaterialTheme.colorScheme.onSurface)
}

@Composable
internal fun Big(text: String, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.primary) {
  Text(text, modifier, fontFamily = Mulish, fontWeight = FontWeight.Black, fontSize = 40.sp, lineHeight = 40.sp, color = color)
}

@Composable
internal fun Chip(text: String) {
  Text(
    text.uppercase(),
    Modifier.background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(50)).padding(horizontal = 10.dp, vertical = 4.dp),
    color = MaterialTheme.colorScheme.onPrimaryContainer, fontFamily = Mulish, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp,
    letterSpacing = 1.sp,
  )
}

@Composable
internal fun Chips(vararg texts: String) {
  Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { texts.forEach { Chip(it) } }
}

@Composable
internal fun Dotted(text: String, modifier: Modifier = Modifier) {
  Row(modifier, horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
    Box(Modifier.size(7.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
    Text(text, fontFamily = Mulish, fontWeight = FontWeight.Bold, fontSize = 16.sp)
  }
}
