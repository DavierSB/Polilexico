package app.lexico.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import app.lexico.online.Lobby
import app.lexico.online.Online
import app.lexico.online.OnlineClient
import app.lexico.online.OnlineStatsRow
import app.lexico.online.Person
import app.lexico.ui.common.ChatContact
import app.lexico.ui.common.ContactGroup
import app.lexico.ui.common.LocalProfileOpener
import app.lexico.ui.common.PlayerProfileDialog
import app.lexico.ui.common.ProfileButtons
import app.lexico.ui.common.ProfileOpener

internal class ProfileContext(val client: OnlineClient, val composer: Composer, val go: (Screen) -> Unit, val inGame: Boolean = false)

@Composable
internal fun ProfileHost(online: Online, context: ProfileContext, content: @Composable () -> Unit) {
  var shown by remember { mutableStateOf<Pair<String, String>?>(null) }
  val opener = remember { ProfileOpener { name, country -> shown = name to country } }
  CompositionLocalProvider(LocalProfileOpener provides opener, content = content)
  shown?.let { (name, country) -> Profile(online, context, name, country) { shown = null } }
}

@Composable
private fun Profile(online: Online, context: ProfileContext, name: String, country: String, close: () -> Unit) {
  val lobby by context.client.lobby.collectAsState()
  val person = lobby.people.find { it.name == name && (country.isEmpty() || it.country == country) }
  val where = country.ifEmpty { person?.country.orEmpty() }
  val loaded by produceState<Result<List<OnlineStatsRow>>?>(null, name, where) {
    value = online.account.token()?.let { online.api.profile(it, name, where) } ?: Result.failure(Exception("La sesión caducó: vuelve a entrar."))
  }
  PlayerProfileDialog(name, where, loaded?.getOrNull()?.let(::statsSections), loaded?.exceptionOrNull()?.message, buttons(context, lobby, person), close)
}

private fun buttons(context: ProfileContext, lobby: Lobby, person: Person?): ProfileButtons {
  person ?: return ProfileButtons()
  val write = { context.composer.write(ChatContact(person.name, person.country, ContactGroup.OTHERS)) }
  if (context.inGame) return ProfileButtons(write = write)
  val game = lobby.games.find { g -> g.players.zip(g.countries).any { it == person.name to person.country } }
  val adjourned = adjournedWith(person, lobby)
  return ProfileButtons(
    write = write,
    watch = game?.let { { context.client.watch(it); context.go(Screen.OnlinePlay) } },
    resume = adjourned?.let { { context.client.resume(it); context.go(Screen.OnlinePlay) } }.takeIf { !person.busy },
    challenge = { context.go(Screen.OnlineNewClassic(person)) }.takeIf { !person.busy && adjourned == null },
  )
}
