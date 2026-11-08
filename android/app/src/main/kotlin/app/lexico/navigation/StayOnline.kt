package app.lexico.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.LifecycleStartEffect
import app.lexico.game.Lexico

@Composable
internal fun StayOnline(lexico: Lexico, hidden: Boolean) {
  val online = rememberOnline()
  val session by online.account.session.collectAsState()
  if (session == null || hidden) return
  val client = remember(online) { online.client(lexico) }
  LifecycleStartEffect(client, session) {
    client.acquire()
    onStopOrDispose { client.release() }
  }
}
