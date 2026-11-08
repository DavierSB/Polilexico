package app.lexico.online

import android.content.Context
import androidx.core.content.edit

const val DEFAULT_SERVER = "https://polilexico1.onrender.com"

data class OnlineSession(val accessToken: String, val refreshToken: String, val expiresAt: Long, val name: String, val country: String)

class OnlineStore(context: Context) {
  private val prefs = context.getSharedPreferences("online", Context.MODE_PRIVATE)

  val server: String = DEFAULT_SERVER

  var ignored: Set<String>
    get() = prefs.getStringSet(IGNORED, null).orEmpty()
    set(value) = prefs.edit { putStringSet(IGNORED, value) }

  var profileOffered: Boolean
    get() = prefs.getBoolean(PROFILE_OFFERED, false)
    set(value) = prefs.edit { putBoolean(PROFILE_OFFERED, value) }

  fun session(): OnlineSession? {
    val access = prefs.getString(ACCESS, null) ?: return null
    val refresh = prefs.getString(REFRESH, null) ?: return null
    return OnlineSession(access, refresh, prefs.getLong(EXPIRES, 0), prefs.getString(NAME, "").orEmpty(), prefs.getString(COUNTRY, "").orEmpty())
  }

  fun save(session: OnlineSession?) = prefs.edit {
    if (session == null) return@edit listOf(ACCESS, REFRESH, EXPIRES, NAME, COUNTRY).forEach { remove(it) }
    putString(ACCESS, session.accessToken)
    putString(REFRESH, session.refreshToken)
    putLong(EXPIRES, session.expiresAt)
    putString(NAME, session.name)
    putString(COUNTRY, session.country)
  }

  private companion object {
    const val IGNORED = "ignored"
    const val PROFILE_OFFERED = "profileOffered"
    const val ACCESS = "access"
    const val REFRESH = "refresh"
    const val EXPIRES = "expires"
    const val NAME = "name"
    const val COUNTRY = "country"
  }
}
