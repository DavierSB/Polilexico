package app.lexico.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun InviteCard(
  invite: SeekItem, onAccept: () -> Unit, onDecline: () -> Unit, modifier: Modifier = Modifier,
  title: String = inviteTitle(invite), labels: Pair<String, String> = "Rechazar" to "Aceptar",
) {
  ElevatedCard(modifier.widthIn(max = 480.dp).fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
      InviteHeader(invite, title)
      InviteButtons(onAccept, onDecline, labels)
    }
  }
}

@Composable
private fun InviteHeader(invite: SeekItem, title: String) {
  Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
    PersonAvatar(invite.host, invite.country, size = 44.dp)
    Column {
      Text(title, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
      Text(invite.detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
  }
}

@Composable
private fun InviteButtons(onAccept: () -> Unit, onDecline: () -> Unit, labels: Pair<String, String>) {
  Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    OutlinedButton(onClick = onDecline, Modifier.weight(1f)) { ButtonText(labels.first) }
    Button(onClick = onAccept, Modifier.weight(1f)) { ButtonText(labels.second) }
  }
}
