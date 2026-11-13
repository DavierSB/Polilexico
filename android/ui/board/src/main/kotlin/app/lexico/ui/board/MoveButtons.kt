package app.lexico.ui.board

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.lexico.ui.common.CHAT_GLYPH
import app.lexico.ui.common.Compact
import app.lexico.ui.common.LocalChat
import app.lexico.ui.common.ButtonText

@Composable
fun MoveButtons(
  c: TilePlacer, playEnabled: Boolean, onPlay: () -> Unit, middle: (@Composable () -> Unit)? = null, small: @Composable RowScope.() -> Unit,
) {
  val chat = LocalChat.current
  Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
      chat?.let { ChatButton(it.onOpen) }
      small()
    }
    middle?.invoke()
    PlayButton(playEnabled && c.hasPlaced, onPlay)
  }
}

@Composable
private fun ChatButton(onOpen: () -> Unit) {
  OutlinedButton(onClick = onOpen, modifier = Modifier.width(48.dp).height(36.dp), contentPadding = Compact) {
    Text(CHAT_GLYPH, fontSize = 16.sp)
  }
}

@Composable
fun RowScope.SmallButton(text: String, enabled: Boolean = true, onClick: () -> Unit) {
  OutlinedButton(onClick = onClick, modifier = Modifier.weight(1f).height(36.dp), enabled = enabled, contentPadding = Compact) {
    ButtonText(text, fontSize = 14.sp)
  }
}

@Composable
fun RowScope.ShuffleOrRecall(c: TilePlacer) =
  SmallButton(if (c.hasPlaced) "Recoger" else "Mezclar") { if (c.hasPlaced) c.recall() else c.shuffle() }

@Composable
private fun PlayButton(enabled: Boolean, onPlay: () -> Unit) {
  Button(onClick = onPlay, modifier = Modifier.fillMaxWidth().height(52.dp), enabled = enabled) {
    Text("Jugar", fontSize = 18.sp, fontWeight = FontWeight.Bold)
  }
}
