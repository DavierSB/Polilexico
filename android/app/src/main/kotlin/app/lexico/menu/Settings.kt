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

/**
 * Las preferencias del usuario: el tema de la app (solo mientras esta abierta), si se cuentan
 * los puntos al colocar, si la bolsa muestra las fichas por salir y desde cuando cuentan las
 * estadisticas.
 */
@Stable
class Settings(ctx: Context) {
  private val prefs = ctx.getSharedPreferences("settings", Context.MODE_PRIVATE)

  /**
   * El tema de la app fuera de las partidas contra un rival. Cada vez que se abre la app sale uno
   * al azar, y dura hasta que se cierra: no se guarda.
   */
  var theme: AppTheme by mutableStateOf(AppThemes.ALL.random())
    private set

  /** Mostrar sobre el tablero los puntos de la jugada mientras se coloca. */
  var liveScore: Boolean by mutableStateOf(prefs.getBoolean(LIVE_SCORE_KEY, true))
    private set

  /** En la clasica, si la bolsa muestra las fichas por salir o solo cuantas quedan. */
  var showUnseen: Boolean by mutableStateOf(prefs.getBoolean(SHOW_UNSEEN_KEY, true))
    private set

  /** Desde cuando cuentan las estadisticas (0 = desde siempre), en milisegundos. */
  var statsSince: Long by mutableLongStateOf(prefs.getLong(STATS_SINCE_KEY, 0L))
    private set

  /** Cambia el tema de la app hasta que se cierre. */
  fun chooseTheme(chosen: AppTheme) {
    theme = chosen
  }

  /** Enciende o apaga los puntos al colocar y lo recuerda. */
  fun showLiveScore(on: Boolean) {
    liveScore = on
    prefs.edit { putBoolean(LIVE_SCORE_KEY, on) }
  }

  /** Muestra u oculta las fichas por salir y lo recuerda. */
  fun showUnseenTiles(on: Boolean) {
    showUnseen = on
    prefs.edit { putBoolean(SHOW_UNSEEN_KEY, on) }
  }

  /** Las estadisticas empiezan de cero: solo cuentan las partidas que se terminen desde ahora. */
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
