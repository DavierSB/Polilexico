package app.lexico.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun NameFlag(name: String, country: String, modifier: Modifier = Modifier, weight: FontWeight? = FontWeight.Bold) {
  Row(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
    Text(name, Modifier.weight(1f, fill = false), fontWeight = weight, maxLines = 1, overflow = TextOverflow.Ellipsis)
    Text(flagOf(country), maxLines = 1, softWrap = false)
  }
}
