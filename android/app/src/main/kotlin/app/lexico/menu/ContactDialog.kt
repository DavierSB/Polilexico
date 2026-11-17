package app.lexico.menu

import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import app.lexico.R

@Composable
fun ContactDialog(version: String, close: () -> Unit) {
  AlertDialog(
    onDismissRequest = close,
    confirmButton = { TextButton(onClick = close) { Text("Cerrar") } },
    title = { Text("Escríbenos") },
    text = { ContactOptions(version) },
  )
}

@Composable
private fun ContactOptions(version: String) {
  val uri = LocalUriHandler.current
  Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
    ContactRow(R.drawable.ic_whatsapp, WHATSAPP_GREEN, "WhatsApp", AUTHOR_PHONE) { uri.openUri(authorChatUrl(version)) }
    ContactRow(R.drawable.ic_mail, MaterialTheme.colorScheme.primary, "Correo", POLILEXICO_EMAIL) { uri.openUri(emailUrl(version)) }
  }
}

@Composable
private fun ContactRow(@DrawableRes icon: Int, tint: Color, label: String, value: String, onClick: () -> Unit) {
  Row(
    Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(12.dp),
    horizontalArrangement = Arrangement.spacedBy(16.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Icon(painterResource(icon), contentDescription = null, tint = tint)
    Column {
      Text(label, style = MaterialTheme.typography.titleSmall)
      Text(value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
  }
}
