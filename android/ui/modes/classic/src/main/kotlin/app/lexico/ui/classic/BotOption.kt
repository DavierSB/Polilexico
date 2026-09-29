package app.lexico.ui.classic

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Un bot para elegir: su foto, su nombre y su descripcion; el elegido, con borde resaltado. */
@Composable
internal fun BotOption(b: Bot, selected: Boolean, select: () -> Unit) {
  Row(Modifier.fillMaxWidth().selectionBorder(selected).clickable(onClick = select).padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
    OpponentPhoto(b.name, 44.dp)
    Column(Modifier.padding(start = 10.dp)) {
      Text(b.alias, fontWeight = FontWeight.Bold)
      Text(b.description, style = MaterialTheme.typography.bodySmall)
    }
  }
}

@Composable
private fun Modifier.selectionBorder(selected: Boolean): Modifier {
  val color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
  return border(if (selected) 2.dp else 1.dp, color, RoundedCornerShape(10.dp))
}
