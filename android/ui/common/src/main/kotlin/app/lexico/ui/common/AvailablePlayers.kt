package app.lexico.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val FULL_ROW = 64.dp
private val COMPACT_ROW = 40.dp
private val TITLE = 28.dp

@Composable
fun TopThenFill(reserve: Dp, top: @Composable () -> Unit, bottom: @Composable () -> Unit) {
  Layout(listOf(top, bottom), Modifier.fillMaxSize()) { (t, b), c ->
    val free = c.copy(minHeight = 0)
    val upper = t.first().measure(free.copy(maxHeight = (c.maxHeight - reserve.roundToPx()).coerceAtLeast(0)))
    val rest = (c.maxHeight - upper.height).coerceAtLeast(0)
    val lower = b.first().measure(free.copy(minHeight = rest, maxHeight = rest))
    layout(c.maxWidth, c.maxHeight) { upper.place(0, 0); lower.place(0, upper.height) }
  }
}

private val GAME_ROW = 42.dp

internal fun lowerReserve(games: Boolean, players: Boolean): Dp =
  (if (games) TITLE + GAME_ROW * 2 else 0.dp) + (if (players) TITLE + FULL_ROW else 0.dp)

@Composable
internal fun LowerSections(games: List<GameItem>, people: List<PersonItem>, actions: PeopleActions) {
  BoxWithConstraints(Modifier.fillMaxSize().padding(top = 8.dp)) {
    val gamesHeight = gamesShare(games, people, maxHeight)
    Column(Modifier.fillMaxSize()) {
      if (games.isNotEmpty()) Box(Modifier.fillMaxWidth().height(gamesHeight)) { OngoingGames(games, actions.watch, gamesHeight) }
      if (people.isNotEmpty()) AvailablePlayers(people, actions)
    }
  }
}

private fun gamesShare(games: List<GameItem>, people: List<PersonItem>, height: Dp): Dp {
  if (people.isEmpty()) return height
  val gamesNeed = TITLE + GAME_ROW * games.size
  val peopleNeed = TITLE + FULL_ROW * people.size
  return minOf(gamesNeed, maxOf(height / 2, height - peopleNeed))
}

@Composable
private fun OngoingGames(games: List<GameItem>, onWatch: (String) -> Unit, height: Dp) {
  var open by remember { mutableStateOf(false) }
  val fit = ((height - TITLE) / GAME_ROW).toInt().coerceAtLeast(1)
  val shown = if (games.size <= fit) games.size else (fit - 1).coerceAtLeast(1)
  Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
    Text("Partidas en curso", style = MaterialTheme.typography.labelLarge, modifier = Modifier.height(TITLE - 6.dp))
    games.take(shown).forEach { CompactGameRow(it) { onWatch(it.id) } }
    if (shown < games.size) MoreButton(games.size - shown) { open = true }
  }
  if (open) GamesDialog(games, { open = false; onWatch(it) }) { open = false }
}

@Composable
internal fun AvailablePlayers(people: List<PersonItem>, actions: PeopleActions) {
  Column(Modifier.fillMaxSize().padding(top = 8.dp)) {
    Text("Retar a", style = MaterialTheme.typography.labelLarge, modifier = Modifier.height(TITLE))
    BoxWithConstraints(Modifier.fillMaxSize()) { FittedPlayers(people, actions, maxHeight) }
  }
}

@Composable
private fun FittedPlayers(people: List<PersonItem>, actions: PeopleActions, height: Dp) {
  val compactRows = (people.size + 1) / 2
  when {
    FULL_ROW * people.size <= height -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { PeopleList(people, actions) }
    COMPACT_ROW * compactRows <= height -> CompactGrid(people)
    else -> CompactWithMore(people, actions, ((height - COMPACT_ROW) / COMPACT_ROW).toInt().coerceAtLeast(0) * 2)
  }
}

@Composable
private fun CompactWithMore(people: List<PersonItem>, actions: PeopleActions, shown: Int) {
  var open by remember { mutableStateOf(false) }
  Column {
    CompactGrid(people.take(shown))
    MoreButton(people.size - shown) { open = true }
  }
  if (open) PeopleDialog(people, closing(actions) { open = false }, "Retar a") { open = false }
}

private fun closing(actions: PeopleActions, close: () -> Unit) = PeopleActions(
  write = { close(); actions.write(it) }, challenge = actions.challenge?.let { c -> { close(); c(it) } }, watch = actions.watch,
)

@Composable
private fun CompactGrid(people: List<PersonItem>) {
  Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
    people.chunked(2).forEach { pair ->
      Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        pair.forEach { CompactPerson(it, Modifier.weight(1f)) }
        if (pair.size == 1) Spacer(Modifier.weight(1f))
      }
    }
  }
}

@Composable
private fun CompactPerson(p: PersonItem, modifier: Modifier) {
  NameFlag(
    p.name, p.country,
    modifier.height(36.dp).clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.surfaceVariant).opensProfile(p.name, p.country)
      .padding(horizontal = 10.dp, vertical = 8.dp),
    weight = null,
  )
}
