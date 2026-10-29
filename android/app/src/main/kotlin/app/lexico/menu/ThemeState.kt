package app.lexico.menu

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.lexico.ui.board.AppTheme
import app.lexico.ui.board.BoardLook
import app.lexico.ui.board.BoardStyle

@Stable
class ThemeState(private val settings: Settings) {
  var game: AppTheme? by mutableStateOf(null)

  var picking by mutableStateOf(false)

  val current: AppTheme get() = game ?: settings.theme

  val appTheme: AppTheme get() = settings.theme

  val board: BoardStyle get() = current.board(settings.boardLook)

  val look: BoardLook get() = settings.boardLook

  fun choose(chosen: AppTheme) {
    if (game != null) game = chosen else settings.chooseTheme(chosen)
  }

  fun chooseLook(look: BoardLook) {
    settings.useLightBoard(look.light)
  }
}
