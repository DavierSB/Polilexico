package app.lexico.ui.classic

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
internal fun WatchPanel(view: ClassicView) {
  Column(
    Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(14.dp)).padding(10.dp),
    verticalArrangement = Arrangement.spacedBy(4.dp),
  ) {
    view.end?.let { Text("${resultTitle(view, it)}   ${view.myScore} – ${view.opponentScore}", fontWeight = FontWeight.Bold) }
    MovesList(view, Modifier.fillMaxWidth().heightIn(max = WATCH_MOVES_HEIGHT))
  }
}

private val WATCH_MOVES_HEIGHT = 150.dp
