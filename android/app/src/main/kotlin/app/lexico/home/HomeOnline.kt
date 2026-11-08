package app.lexico.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import app.lexico.navigation.Composer
import app.lexico.navigation.MessageDialogs
import app.lexico.navigation.ProvidePhotos
import app.lexico.navigation.Screen
import app.lexico.navigation.contacts
import app.lexico.navigation.personItem
import app.lexico.navigation.shuffledPeople
import app.lexico.online.Lobby
import app.lexico.online.Online
import app.lexico.online.OnlineClient
import app.lexico.online.Person
import app.lexico.ui.common.ChatContact
import app.lexico.ui.common.ContactGroup
import app.lexico.ui.common.MessagesButton
import app.lexico.ui.common.PeopleActions
import app.lexico.ui.common.PeopleDialog

@Composable
internal fun HomeOnline(presence: Int?, compact: Boolean, go: (Screen) -> Unit) {
  val online = Online.of(LocalContext.current)
  val session by online.account.session.collectAsState()
  val client by online.current.collectAsState()
  val current = client.takeIf { session != null }
  if (current == null) return OnlineChip(presence, compact) { go(Screen.OnlineAccount) }
  SignedIn(online, current, presence, compact, go)
}

private fun peopleActions(client: OnlineClient, lobby: Lobby, composer: Composer, go: (Screen) -> Unit, close: () -> Unit) = PeopleActions(
  write = { close(); composer.write(ChatContact(it.name, it.country, ContactGroup.OTHERS)) },
  challenge = { close(); go(Screen.OnlineNewClassic(Person(it.name, it.country, it.busy))) },
  watch = { id -> lobby.games.find { it.id == id }?.let { close(); client.watch(it); go(Screen.OnlinePlay) } },
)

@Composable
private fun SignedIn(online: Online, client: OnlineClient, presence: Int?, compact: Boolean, go: (Screen) -> Unit) {
  val lobby by client.lobby.collectAsState()
  val inbox by client.inbox.collectAsState()
  val composer = remember { Composer() }
  var showingPeople by remember { mutableStateOf(false) }
  MessagesButton(inbox.size) { composer.write() }
  OnlineChip(presence, compact) { showingPeople = true }
  ProvidePhotos(online, lobby) {
    if (showingPeople) PeopleDialog(shuffledPeople(lobby.people).map { personItem(it, lobby.games) }, peopleActions(client, lobby, composer, go) { showingPeople = false }) {
      showingPeople = false
    }
    MessageDialogs(client, contacts(lobby, emptyList(), emptyList()), null, composer)
  }
}
