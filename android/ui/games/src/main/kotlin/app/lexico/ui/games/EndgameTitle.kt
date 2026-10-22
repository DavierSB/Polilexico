package app.lexico.ui.games

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.lexico.ui.common.Compact

private val NAV_WIDTH = 44.dp
private val NAV_HEIGHT = 36.dp

@Composable
internal fun ExploreTitle(review: ReviewView, index: Int, path: List<StepView>, go: (Int) -> Unit, back: (Int) -> Unit) {
  val turn = review.turns[index]
  Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(4.dp), Alignment.CenterVertically) {
    NavButton("‹", index > 0) { go(index - 1) }
    Box(Modifier.weight(1f)) { if (path.isEmpty()) TurnTitle(turn, review.turns.size) else Breadcrumb(turn, path, back) }
    if (path.isNotEmpty()) TextButton(onClick = { back(1) }, contentPadding = Compact) { Text("Deshacer") }
    NavButton("›", index < review.turns.lastIndex) { go(index + 1) }
  }
}

@Composable
private fun NavButton(symbol: String, enabled: Boolean, onClick: () -> Unit) {
  OutlinedButton(onClick = onClick, Modifier.size(NAV_WIDTH, NAV_HEIGHT), enabled = enabled, contentPadding = PaddingValues(0.dp)) { Text(symbol) }
}

@Composable
private fun Breadcrumb(turn: ReviewTurnView, path: List<StepView>, back: (Int) -> Unit) {
  val scroll = rememberScrollState()
  LaunchedEffect(path.size) { scroll.animateScrollTo(scroll.maxValue) }
  Row(Modifier.horizontalScroll(scroll), Arrangement.spacedBy(4.dp), Alignment.CenterVertically) {
    PathChip("Turno ${turn.number}", active = false) { back(path.size) }
    path.forEachIndexed { i, step -> PathChip("${step.move.text} ${step.move.points}", active = i == path.lastIndex) { back(path.lastIndex - i) } }
  }
}

@Composable
private fun PathChip(text: String, active: Boolean, select: () -> Unit) {
  val colors = MaterialTheme.colorScheme
  Text(
    text, Modifier.background(if (active) colors.primary else colors.surfaceVariant, RoundedCornerShape(50)).clickable(onClick = select)
      .padding(horizontal = 10.dp, vertical = 4.dp),
    color = if (active) colors.onPrimary else colors.onSurfaceVariant, style = MaterialTheme.typography.labelLarge, maxLines = 1,
  )
}
