package app.lexico.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import app.lexico.online.Lobby
import app.lexico.online.OnlineClient
import app.lexico.online.Person
import app.lexico.ui.common.ChatComposer
import app.lexico.ui.common.ChatHistoryDialog
import app.lexico.ui.common.ROOM_CONTACT
import app.lexico.ui.common.ChatContact
import app.lexico.ui.common.ChatLine
import app.lexico.ui.common.ChatPopup
import app.lexico.ui.common.ContactGroup

private val WITH_COUNTRY = Regex("^(.*) \\(([A-Z]{2})\\)$")

@Stable
internal class Composer {
  var open by mutableStateOf(false)
  var preset by mutableStateOf<ChatContact?>(null)

  fun write(to: ChatContact? = null) {
    preset = to
    open = true
  }
}

@Composable
internal fun MessageDialogs(client: OnlineClient, contacts: List<ChatContact>, fallback: ChatContact?, composer: Composer) {
  val inbox by client.inbox.collectAsState()
  var history by remember { mutableStateOf(false) }
  if (!composer.open) ChatPopup(
    inbox, client::closeChat, onReply = { line -> client.closeChat(); composer.write(replyTo(line, contacts)) },
    onIgnoreUser = { client.ignorePerson(it.from, it.country) }, onIgnoreAll = { client.ignore(true) },
  )
  if (composer.open) Composing(client, contacts, fallback, composer) { history = true }
  if (history) ChatHistoryDialog(rememberOnline().chatLog.records.collectAsState().value) { history = false }
}

@Composable
private fun Composing(client: OnlineClient, contacts: List<ChatContact>, fallback: ChatContact?, composer: Composer, onHistory: () -> Unit) {
  val ignoring by client.ignoring.collectAsState()
  val lastTo by client.lastTo.collectAsState()
  val room = contacts.find { it.group == ContactGroup.ROOM }
  val initial = composer.preset ?: room ?: lastTo?.let { last -> contacts.find { it.name == last.name && it.country == last.country } } ?: fallback
  ChatComposer(initial, contacts, ignoring, { to, text -> send(client, to, text) }, { client.ignore(false) }, onHistory) { composer.open = false }
}

private fun send(client: OnlineClient, to: ChatContact, text: String) =
  if (to.group == ContactGroup.ROOM) client.tellRoom(text) else client.tell(Person(to.name, to.country, false), text)

internal fun contacts(lobby: Lobby, rivals: List<String>, watchers: List<Person>): List<ChatContact> {
  val rivalNames = rivals.map(::plainName).toSet()
  return lobby.people.map { p ->
    val group = when {
      p.name in rivalNames -> ContactGroup.RIVALS
      watchers.any { it.name == p.name && it.country == p.country } -> ContactGroup.WATCHERS
      else -> ContactGroup.OTHERS
    }
    ChatContact(p.name, p.country, group)
  }.sortedWith(compareBy({ it.group.ordinal }, { it.name.lowercase() }))
}

internal fun plainName(seat: String): String = WITH_COUNTRY.matchEntire(seat)?.groupValues?.get(1) ?: seat

private fun replyTo(line: ChatLine, contacts: List<ChatContact>): ChatContact =
  if (line.room) ROOM_CONTACT else contacts.find { it.name == line.from && it.country == line.country } ?: ChatContact(line.from, line.country, ContactGroup.OTHERS)
