package app.lexico.menu

import android.content.Context
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import app.lexico.ui.board.AppTheme
import app.lexico.ui.board.AppThemes

@Stable
class Settings(ctx: Context) {
  private val prefs = ctx.getSharedPreferences("settings", Context.MODE_PRIVATE)

  var theme: AppTheme by mutableStateOf(AppThemes.ALL.random())
    private set

  var liveScore: Boolean by mutableStateOf(prefs.getBoolean(LIVE_SCORE_KEY, true))
    private set

  var showUnseen: Boolean by mutableStateOf(prefs.getBoolean(SHOW_UNSEEN_KEY, true))
    private set

  var statsSince: Long by mutableLongStateOf(prefs.getLong(STATS_SINCE_KEY, 0L))
    private set

  fun chooseTheme(chosen: AppTheme) {
    theme = chosen
  }

  fun showLiveScore(on: Boolean) {
    liveScore = on
    prefs.edit { putBoolean(LIVE_SCORE_KEY, on) }
  }

  fun showUnseenTiles(on: Boolean) {
    showUnseen = on
    prefs.edit { putBoolean(SHOW_UNSEEN_KEY, on) }
  }

  fun resetStats() {
    statsSince = System.currentTimeMillis()
    prefs.edit { putLong(STATS_SINCE_KEY, statsSince) }
  }

  private companion object {
    const val LIVE_SCORE_KEY = "liveScore"
    const val SHOW_UNSEEN_KEY = "showUnseen"
    const val STATS_SINCE_KEY = "statsSince"
  }
}
