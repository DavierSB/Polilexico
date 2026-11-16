package app.lexico.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun ZoomableAvatar(name: String, country: String, size: Dp) {
  val image = LocalPhotos.current?.of(name, country)
  var zoomed by remember { mutableStateOf(false) }
  val tap = if (image != null) Modifier.clip(CircleShape).clickable { zoomed = true } else Modifier
  Box(tap) { PlayerAvatar(name, highlighted = false, size, image) }
  if (zoomed && image != null) PhotoZoom(image, name, country) { zoomed = false }
}

@Composable
private fun PhotoZoom(image: ImageBitmap, name: String, country: String, close: () -> Unit) {
  Dialog(close, DialogProperties(usePlatformDefaultWidth = false)) {
    Box(Modifier.fillMaxSize().background(ZOOM_SCRIM).tapToClose(close), contentAlignment = Alignment.Center) {
      Column(Modifier.padding(16.dp), Arrangement.spacedBy(16.dp), Alignment.CenterHorizontally) {
        PhotoCircle(image, name, zoomSide())
        Text("$name  ${flagOf(country)}", style = MaterialTheme.typography.titleLarge, color = Color.White, textAlign = TextAlign.Center)
      }
    }
  }
}

@Composable
private fun Modifier.tapToClose(close: () -> Unit): Modifier =
  clickable(remember { MutableInteractionSource() }, indication = null, onClick = close)

@Composable
private fun zoomSide(): Dp {
  val screen = LocalConfiguration.current
  return (minOf(screen.screenWidthDp, screen.screenHeightDp) * 0.8f).dp.coerceAtMost(MAX_ZOOM)
}

private val ZOOM_SCRIM = Color.Black.copy(alpha = 0.85f)
private val MAX_ZOOM = 420.dp
