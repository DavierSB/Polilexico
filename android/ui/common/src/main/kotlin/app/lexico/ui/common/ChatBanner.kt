package app.lexico.ui.common

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties

const val CHAT_BANNER_MS = 6000

@Composable
fun ChatBanner(lines: List<ChatLine>, onSee: () -> Unit, onClose: () -> Unit, onTimeout: () -> Unit) {
  val last = lines.lastOrNull() ?: return
  val progress = remember { Animatable(0f) }
  LaunchedEffect(last) {
    progress.snapTo(0f)
    progress.animateTo(1f, tween(CHAT_BANNER_MS, easing = LinearEasing))
    onTimeout()
  }
  Popup(alignment = Alignment.TopCenter, properties = PopupProperties(focusable = false)) {
    BannerCard(last, lines.size, { 1f - progress.value }, onSee, onClose)
  }
}

@Composable
private fun BannerCard(last: ChatLine, count: Int, remaining: () -> Float, onSee: () -> Unit, onClose: () -> Unit) {
  Surface(
    Modifier.statusBarsPadding().padding(horizontal = 8.dp, vertical = 6.dp).widthIn(max = 520.dp).fillMaxWidth(),
    shape = RoundedCornerShape(14.dp), tonalElevation = 6.dp, shadowElevation = 10.dp,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
  ) {
    Column {
      BannerRow(last, count, onSee, onClose)
      LinearProgressIndicator(progress = remaining, Modifier.fillMaxWidth().padding(horizontal = 20.dp).height(2.dp), drawStopIndicator = {})
    }
  }
}

@Composable
private fun BannerRow(last: ChatLine, count: Int, onSee: () -> Unit, onClose: () -> Unit) {
  Row(Modifier.padding(start = 10.dp, top = 8.dp, bottom = 6.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
    PersonAvatar(last.from, last.country, size = 36.dp)
    Column(Modifier.weight(1f)) {
      BannerTitle(last, count)
      Text("“${last.text}”", fontStyle = FontStyle.Italic, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    Button(onClick = onSee) { Text("Ver") }
    IconButton(onClick = onClose) { Text("✕", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
  }
}

@Composable
private fun BannerTitle(last: ChatLine, count: Int) {
  Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
    Text(if (last.room) "${last.from} → sala" else last.from, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    if (count > 1) CountChip(count)
  }
}

@Composable
private fun CountChip(count: Int) {
  Box(Modifier.background(MaterialTheme.colorScheme.primary, RoundedCornerShape(50)).padding(horizontal = 6.dp)) {
    Text("$count", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
  }
}
