package app.lexico.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.lexico.model.Placement
import java.util.Locale

val LocalWordLookup = staticCompositionLocalOf<((String) -> Unit)?> { null }

@Composable
fun DictionaryWord(word: String, modifier: Modifier = Modifier, fontSize: TextUnit = 26.sp) {
  val lookup = LocalWordLookup.current
  Text(
    word.uppercase(Locale.ROOT), modifier.clip(RoundedCornerShape(8.dp)).clickable(enabled = lookup != null) { lookup?.invoke(word) }
      .padding(horizontal = 10.dp),
    fontSize = fontSize, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp,
  )
}

@Composable
fun LookupButton(word: String, size: Dp = 36.dp) {
  val lookup = LocalWordLookup.current ?: return
  IconButton({ lookup(word) }, Modifier.size(size)) {
    Icon(LexicoIcons.Lookup, "Ver en el diccionario", Modifier.size(size * 0.55f), tint = MaterialTheme.colorScheme.primary)
  }
}

val PLAY_LOOKUP_WIDTH = 30.dp

@Composable
fun PlayLookup(play: String) {
  Box(Modifier.width(PLAY_LOOKUP_WIDTH), Alignment.Center) { playedWord(play)?.let { LookupButton(it, PLAY_LOOKUP_WIDTH) } }
}

@Composable
fun Modifier.lookUpOnLongPress(play: String): Modifier {
  val lookup = LocalWordLookup.current ?: return this
  val word = playedWord(play) ?: return this
  val haptic = LocalHapticFeedback.current
  return pointerInput(word, lookup) { awaitLongPress { haptic.performHapticFeedback(HapticFeedbackType.LongPress); lookup(word) } }
}

suspend fun PointerInputScope.awaitLongPress(onLongPress: (Offset) -> Unit) = awaitEachGesture {
  val down = awaitFirstDown(requireUnconsumed = false)
  val ended = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) { waitForUpOrCancellation(); true }
  if (ended == null) onLongPress(down.position)
}

fun playedWord(play: String): String? {
  if (Placement.parseOrNull(play) == null) return null
  return play.trim().split(Regex("\\s+"))[1].takeIf { word -> word.all { it.isLetter() } }
}
