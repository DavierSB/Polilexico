package app.lexico.menu

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.lexico.ui.board.AppTheme

/**
 * El tema en pantalla: en una partida, el de esa partida (el de su rival o el de la app al
 * empezar, o el que se eligio en ella); fuera de las partidas, el de la app. Elegir un tema en
 * una partida solo cambia esa partida; el de la app solo se cambia desde el menu lateral.
 */
@Stable
class ThemeState(private val settings: Settings) {
  /** El tema de la partida en pantalla; `null` fuera de las partidas. */
  var game: AppTheme? by mutableStateOf(null)

  /** Si esta abierto el dialogo para elegir tema. */
  var picking by mutableStateOf(false)

  val current: AppTheme get() = game ?: settings.theme

  /** El de la app, con el que empiezan las partidas que no son contra un rival. */
  val appTheme: AppTheme get() = settings.theme

  fun choose(chosen: AppTheme) {
    if (game != null) game = chosen else settings.chooseTheme(chosen)
  }
}
