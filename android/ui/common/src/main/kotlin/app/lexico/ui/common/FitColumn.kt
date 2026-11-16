package app.lexico.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import kotlin.math.abs

private const val MIN_SCALE = 0.6f

private const val SCALE_STEP = 0.01f

private const val BOUNCE_MS = 1000L

@Composable
fun FitColumn(gap: Dp, content: @Composable ColumnScope.() -> Unit) {
  val scale = remember { mutableFloatStateOf(1f) }
  val keyboard = WindowInsets.ime.getBottom(LocalDensity.current)
  BoxWithConstraints(Modifier.fillMaxSize()) {
    val room = constraints.maxHeight + keyboard
    CompositionLocalProvider(LocalDensity provides scaled(LocalDensity.current, scale.floatValue)) {
      Column(
        Modifier.verticalScroll(rememberScrollState()).onSizeChanged { fit(scale, room, it.height) },
        verticalArrangement = Arrangement.spacedBy(gap), content = content,
      )
    }
  }
}

internal fun scaled(density: Density, scale: Float, maxFontScale: Float = density.fontScale): Density =
  Density(density.density * scale, minOf(density.fontScale, maxFontScale))

internal class Refit {
  val scale = mutableFloatStateOf(1f)
  private var room = 0
  private var ceiling = 1f
  private var grewAt = 0L

  fun update(room: Int, needed: Int) {
    if (needed <= 0) return
    if (room != this.room) reset(room)
    val wanted = (scale.floatValue * room / needed).coerceIn(MIN_SCALE, ceiling)
    if (abs(scale.floatValue - wanted) > SCALE_STEP) apply(wanted)
  }

  private fun reset(room: Int) {
    this.room = room
    ceiling = 1f
  }

  private fun apply(wanted: Float) {
    val now = System.currentTimeMillis()
    if (wanted > scale.floatValue) grewAt = now
    else if (now - grewAt < BOUNCE_MS) ceiling = wanted
    scale.floatValue = wanted
  }
}

internal fun fit(scale: MutableFloatState, room: Int, needed: Int) {
  if (needed <= 0) return
  val wanted = (scale.floatValue * room / needed).coerceAtLeast(MIN_SCALE)
  if (scale.floatValue - wanted > SCALE_STEP) scale.floatValue = wanted
}
