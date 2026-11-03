package app.lexico.online

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private const val REFRESH_MARGIN_MS = 5 * 60_000L

class OnlineAccount(private val store: OnlineStore, private val api: OnlineApi) {
  private val _session = MutableStateFlow(store.session())
  val session: StateFlow<OnlineSession?> = _session.asStateFlow()
  private val refreshing = Mutex()

  suspend fun signUp(form: AccountForm): String? = adopt(api.signUp(form))

  suspend fun logIn(form: AccountForm): String? = adopt(api.logIn(form))

  fun logOut() {
    store.save(null)
    _session.value = null
  }

  suspend fun token(): String? = refreshing.withLock {
    val current = _session.value ?: return null
    if (current.expiresAt - System.currentTimeMillis() > REFRESH_MARGIN_MS) return current.accessToken
    api.refresh(current.refreshToken).fold({ keep(it).accessToken }) { forgetIfRejected(it); null }
  }

  private fun adopt(result: Result<OnlineSession>): String? = result.fold({ keep(it); null }) { it.message }

  private fun keep(session: OnlineSession): OnlineSession {
    store.save(session)
    _session.value = session
    return session
  }

  private fun forgetIfRejected(error: Throwable) {
    if (error.message != null && error.message != OFFLINE) logOut()
  }
}
