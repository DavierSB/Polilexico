package app.lexico.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val TRACK_HEIGHT = 64.dp
private val LINE_TOP = 26.dp
private val BELOW_TOP = 44.dp
private val DOT = 22.dp
private val DOT_SIZE = 16.dp
private val LABEL_GAP = 6.dp

@Composable
fun RaceTrack(board: RaceBoard) {
  var shown by remember { mutableStateOf<TrackSpot?>(null) }
  Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
    Text("0", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
    BoxWithConstraints(Modifier.weight(1f).height(TRACK_HEIGHT).padding(horizontal = DOT / 2)) {
      Track(board, constraints.maxWidth.toFloat()) { shown = it }
    }
    Text("🏁 ${board.goal}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
  }
  shown?.let { TiedDialog(it, board) { shown = null } }
}

@Composable
private fun Track(board: RaceBoard, width: Float, onTied: (TrackSpot) -> Unit) {
  val density = LocalDensity.current
  val measurer = rememberTextMeasurer()
  val style = labelStyle()
  val spots = remember(board, width) {
    with(density) {
      TrackLayout.of(board.runners, board.goal, width, DOT.toPx(), LABEL_GAP.toPx()) { measurer.measure(it, style).size.width.toFloat() }
    }
  }
  Line(myX(board, spots), width)
  spots.forEach { Spot(it, board.me, onTied) }
}

@Composable
private fun Line(mine: Float?, width: Float) {
  val dp = with(LocalDensity.current) { (mine ?: 0f).toDp() }
  val total = with(LocalDensity.current) { width.toDp() }
  Box(Modifier.offset(y = LINE_TOP - 3.dp).size(total, 6.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape))
  if (mine != null) Box(Modifier.offset(y = LINE_TOP - 3.dp).size(dp, 6.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.6f), CircleShape))
}

private fun myX(board: RaceBoard, spots: List<TrackSpot>): Float? = spots.firstOrNull { s -> s.runners.any { it.name == board.me } }?.x

@Composable
private fun Spot(spot: TrackSpot, me: String, onTied: (TrackSpot) -> Unit) {
  val density = LocalDensity.current
  val mine = spot.runners.any { it.name == me }
  val x = with(density) { spot.x.toDp() }
  val tied = spot.runners.size > 1
  Dot(spot, mine, Modifier.offset(x = x - DOT / 2, y = LINE_TOP - DOT / 2).then(if (tied) Modifier.clickable { onTied(spot) } else Modifier))
  Text(
    spot.label, Modifier.offset(x = with(density) { spot.labelX.toDp() }, y = if (spot.above) 0.dp else BELOW_TOP),
    style = labelStyle(), maxLines = 1, softWrap = false,
    color = if (mine) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
    fontWeight = if (mine) FontWeight.ExtraBold else FontWeight.Normal,
  )
}

@Composable
private fun Dot(spot: TrackSpot, mine: Boolean, modifier: Modifier) {
  val colors = MaterialTheme.colorScheme
  val fill: Color = when {
    mine -> colors.primary
    spot.runners.all { it.done || !it.present } -> colors.outline
    else -> colors.onSurfaceVariant
  }
  Box(Modifier.size(DOT).then(modifier), contentAlignment = Alignment.Center) {
    Box(Modifier.size(DOT_SIZE).background(fill, CircleShape))
  }
}

@Composable
private fun TiedDialog(spot: TrackSpot, board: RaceBoard, onClose: () -> Unit) {
  AlertDialog(
    onDismissRequest = onClose,
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        spot.runners.sortedByDescending { it.score }.forEach { r ->
          Row(Modifier.fillMaxWidth()) {
            Text(r.name, Modifier.weight(1f), fontWeight = if (r.name == board.me) FontWeight.Bold else FontWeight.Normal)
            Text(board.count(r.score))
          }
        }
      }
    },
    confirmButton = { TextButton(onClick = onClose) { Text("Cerrar") } },
  )
}

@Composable
private fun labelStyle(): TextStyle = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
