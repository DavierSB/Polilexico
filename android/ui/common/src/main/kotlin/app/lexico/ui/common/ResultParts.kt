package app.lexico.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

data class ResultChip(val text: String, val highlighted: Boolean = false)

enum class Mark { HIT, MISS, NONE }

@Composable
fun ResultCard(label: String, value: String, caption: String, chips: List<ResultChip> = emptyList(), progress: Float? = null) {
  Surface(Modifier.fillMaxWidth(), RoundedCornerShape(20.dp), MaterialTheme.colorScheme.surfaceContainerHigh) {
    Column(Modifier.padding(16.dp), Arrangement.spacedBy(4.dp), Alignment.CenterHorizontally) {
      SmallCaps(label)
      Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(value, fontFamily = Mulish, fontWeight = FontWeight.Black, fontSize = 52.sp, lineHeight = 52.sp)
        Text(caption, Modifier.padding(bottom = 8.dp), fontFamily = Mulish, fontWeight = FontWeight.Bold, fontSize = 17.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
      progress?.let { Progress(it) }
      if (chips.isNotEmpty()) Row(Modifier.padding(top = 6.dp), Arrangement.spacedBy(8.dp)) { chips.forEach { Chip(it) } }
    }
  }
}

@Composable
fun ResultButtons(primary: String, onPrimary: () -> Unit, secondary: String, onSecondary: () -> Unit) {
  Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp)) {
    Button(onClick = onPrimary, Modifier.weight(1f).height(46.dp)) { Text(primary, fontWeight = FontWeight.Bold) }
    OutlinedButton(onClick = onSecondary, Modifier.weight(1f).height(46.dp)) { Text(secondary, fontWeight = FontWeight.Bold) }
  }
}

@Composable
fun ResultSection(title: String, hint: String? = null) {
  Row(Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, top = 6.dp, bottom = 2.dp), verticalAlignment = Alignment.Bottom) {
    SmallCaps(title.uppercase(Locale.ROOT), Modifier.weight(1f))
    hint?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
  }
}

@Composable
fun WordRow(
  word: String,
  display: AnnotatedString = AnnotatedString(word.uppercase(Locale.ROOT)),
  tag: String? = null,
  score: Int? = null,
  mark: Mark? = null,
  selected: Boolean = false,
  onClick: (() -> Unit)? = null,
) {
  val lookup = LocalWordLookup.current
  val tap = onClick ?: lookup?.let { { it(word) } }
  val background = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerLow
  Row(
    Modifier.fillMaxWidth().height(46.dp).clip(RoundedCornerShape(12.dp)).background(background).clickable(enabled = tap != null) { tap?.invoke() }
      .padding(start = 8.dp, end = 2.dp),
    Arrangement.spacedBy(8.dp), Alignment.CenterVertically,
  ) {
    mark?.let { MarkBadge(it) }
    tag?.let { Text(it, Modifier.width(34.dp), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    Text(display, Modifier.weight(1f), fontSize = 20.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, maxLines = 1,
      color = if (mark == Mark.MISS) MaterialTheme.colorScheme.onSurfaceVariant else Color.Unspecified)
    score?.let { ScorePill(it) }
    if (onClick != null) LookupButton(word) else LookupHint(lookup != null)
  }
}

@Composable
fun OutcomeBanner(title: String, main: String, detail: String = "", good: Boolean) {
  val (container, accent) = if (good) HIT_CONTAINER to HIT_INK else MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.error
  Surface(Modifier.fillMaxWidth(), RoundedCornerShape(16.dp), container) {
    Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), Arrangement.spacedBy(12.dp), Alignment.CenterVertically) {
      Box(Modifier.size(40.dp).background(accent, CircleShape), Alignment.Center) {
        Text(if (good) "✓" else "✗", color = container, fontWeight = FontWeight.Black, fontSize = 22.sp)
      }
      Column(Modifier.weight(1f), Arrangement.spacedBy(2.dp)) {
        SmallCaps(title, color = accent)
        Row(verticalAlignment = Alignment.Bottom) {
          Text(main, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
          if (detail.isNotEmpty()) Text(detail, Modifier.padding(start = 6.dp, bottom = 2.dp), fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
      }
    }
  }
}

@Composable
private fun SmallCaps(text: String, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.primary) {
  Text(text, modifier, fontFamily = Mulish, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, letterSpacing = 1.5.sp, color = color)
}

@Composable
private fun Progress(fraction: Float) {
  LinearProgressIndicator(
    progress = { fraction }, Modifier.fillMaxWidth().padding(top = 4.dp).height(8.dp).clip(RoundedCornerShape(4.dp)),
    color = HIT_INK, trackColor = MaterialTheme.colorScheme.surfaceContainerHighest, drawStopIndicator = {},
  )
}

@Composable
private fun Chip(chip: ResultChip) {
  val (background, ink) = if (chip.highlighted) MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
  else MaterialTheme.colorScheme.surfaceContainerHighest to MaterialTheme.colorScheme.onSurfaceVariant
  Text(chip.text, Modifier.background(background, RoundedCornerShape(50)).padding(horizontal = 12.dp, vertical = 4.dp),
    color = ink, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
}

@Composable
private fun ScorePill(score: Int) {
  Text("$score", Modifier.background(MaterialTheme.colorScheme.surfaceContainerHighest, RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 2.dp),
    fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun MarkBadge(mark: Mark) {
  val (background, ink, sign) = when (mark) {
    Mark.HIT -> Triple(HIT_CONTAINER, HIT_INK, "✓")
    Mark.MISS -> Triple(MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.error, "✗")
    Mark.NONE -> Triple(MaterialTheme.colorScheme.surfaceContainerHighest, MaterialTheme.colorScheme.onSurfaceVariant, "·")
  }
  Box(Modifier.size(26.dp).background(background, CircleShape), Alignment.Center) {
    Text(sign, color = ink, fontWeight = FontWeight.Black, fontSize = 14.sp)
  }
}

@Composable
private fun LookupHint(shown: Boolean) {
  if (!shown) return
  Box(Modifier.size(36.dp), Alignment.Center) {
    Icon(LexicoIcons.Lookup, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
  }
}

private val HIT_CONTAINER = Color(0xFF1E3A20)
private val HIT_INK = Color(0xFF7BC47F)
