package app.lexico.ui.duplicate

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.unit.sp

@Composable
internal fun ProposalCard(proposal: SeatEntry?) {
  Row(
    Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(14.dp)).padding(horizontal = 12.dp, vertical = 8.dp),
    horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically,
  ) {
    if (proposal == null) return@Row Text("Aún no has jugado", color = MaterialTheme.colorScheme.onSurfaceVariant)
    Column(Modifier.weight(1f)) {
      Text("Tu jugada", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
      Played(proposal.text)
    }
    PointsPill(proposal.points)
  }
}

@Composable
private fun Played(text: String) {
  val coords = text.substringBefore(' ', "")
  Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
    if (coords.isNotEmpty()) Text(coords, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Text(text.substringAfter(' '), fontSize = 20.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
  }
}

@Composable
private fun PointsPill(points: Int) {
  Text(
    "$points pts", Modifier.background(MaterialTheme.colorScheme.primary, RoundedCornerShape(50)).padding(horizontal = 12.dp, vertical = 6.dp),
    color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold,
  )
}
