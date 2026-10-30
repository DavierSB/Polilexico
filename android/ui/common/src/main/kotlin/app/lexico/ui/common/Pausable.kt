package app.lexico.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

private const val COVER_ALPHA = 0.92f

@Composable
fun Pausable(paused: Boolean, onResume: (() -> Unit)?, note: String? = null, content: @Composable () -> Unit) {
  Covered(paused, COVER_ALPHA, content) {
    Text("Juego pausado", style = MaterialTheme.typography.headlineSmall)
    note?.let { Text(it, textAlign = TextAlign.Center) }
    onResume?.let { Button(onClick = it) { Text("Continuar") } }
  }
}

@Composable
fun Covered(covered: Boolean, alpha: Float, content: @Composable () -> Unit, cover: @Composable ColumnScope.() -> Unit) {
  Box {
    Box(if (covered) Modifier.blur(16.dp) else Modifier) { content() }
    if (covered) Cover(alpha, cover)
  }
}

@Composable
private fun BoxScope.Cover(alpha: Float, cover: @Composable ColumnScope.() -> Unit) {
  Column(
    Modifier.matchParentSize().background(MaterialTheme.colorScheme.surface.copy(alpha = alpha)).swallowTaps().padding(24.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    horizontalAlignment = Alignment.CenterHorizontally,
    content = cover,
  )
}

@Composable
private fun Modifier.swallowTaps(): Modifier =
  clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
