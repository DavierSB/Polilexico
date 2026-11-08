package app.lexico.home

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import app.lexico.game.storage.Mode
import app.lexico.navigation.Screen
import app.lexico.online.Online

@Composable
internal fun rememberProfileOffer(presence: Int?, go: (Screen) -> Unit): (Screen) -> Unit {
  val online = Online.of(LocalContext.current)
  val session by online.account.session.collectAsState()
  var pending by remember { mutableStateOf<Screen?>(null) }
  pending?.let { target -> ProfileOfferDialog(presence ?: 0, { answer(online, target, true, go); pending = null }) { answer(online, target, false, go); pending = null } }
  return { target -> if (session == null && shouldOffer(online, target, presence)) pending = target else go(target) }
}

private fun shouldOffer(online: Online, target: Screen, presence: Int?): Boolean =
  modeOf(target) != null && !online.store.profileOffered && (presence ?: 0) > 0

private fun answer(online: Online, target: Screen, create: Boolean, go: (Screen) -> Unit) {
  online.store.profileOffered = true
  go(target)
  if (create) go(Screen.OnlineAccount)
}

private fun modeOf(screen: Screen): Mode? = when (screen) {
  Screen.NewClassic -> Mode.CLASSIC
  Screen.NewDuplicate -> Mode.DUPLICATE
  else -> null
}

@Composable
private fun ProfileOfferDialog(presence: Int, onCreate: () -> Unit, onLater: () -> Unit) {
  AlertDialog(
    onDismissRequest = onLater,
    icon = { Text("👥") },
    title = { Text("Jugar en línea") },
    text = { Text(if (presence == 1) "Hay 1 persona conectada." else "Hay $presence personas conectadas.") },
    confirmButton = { Button(onClick = onCreate) { Text("Crear perfil") } },
    dismissButton = { TextButton(onClick = onLater) { Text("Ahora no") } },
  )
}
