package app.lexico.navigation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import app.lexico.game.Lexico
import app.lexico.online.Lobby
import app.lexico.online.OnlineSession
import app.lexico.ui.common.MessagesButton
import app.lexico.ui.common.PersonAvatar

@Composable
internal fun AccountButtons(session: OnlineSession, lexico: Lexico, onOpen: () -> Unit) {
  HeaderMessages(lexico)
  ProfileButton(session, onOpen)
}

@Composable
private fun HeaderMessages(lexico: Lexico) {
  val client = rememberClient(lexico)
  val inbox by client.inbox.collectAsState()
  val lobby by client.lobby.collectAsState()
  val composer = remember { Composer() }
  MessagesButton(inbox.size) { composer.write() }
  ComposerDialogs(client, contacts(lobby, emptyList(), emptyList()), null, composer)
}

@Composable
internal fun ProfileButton(session: OnlineSession, onOpen: () -> Unit) {
  ProvidePhotos(rememberOnline(), Lobby()) {
    Box(Modifier.size(40.dp).clip(CircleShape).clickable(onClick = onOpen), contentAlignment = Alignment.Center) {
      PersonAvatar(session.name, session.country, highlighted = true, size = 32.dp)
    }
  }
}

internal val Screen.showsProfile: Boolean
  get() = this == Screen.NewClassic || this == Screen.NewDuplicate
