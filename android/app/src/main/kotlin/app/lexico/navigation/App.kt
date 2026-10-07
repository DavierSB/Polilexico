package app.lexico.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Surface
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.lexico.game.Lexico
import app.lexico.menu.AboutDialog
import app.lexico.menu.MenuState
import app.lexico.menu.OptionsDialog
import app.lexico.menu.Settings
import app.lexico.menu.SideMenu
import app.lexico.menu.ThemeState
import app.lexico.menu.rememberMenuState
import app.lexico.modes.scoring.engineScorer
import app.lexico.game.Cue
import app.lexico.sound.CuePlayer
import app.lexico.sound.LocalCues
import app.lexico.ui.board.LocalPlayScorer
import app.lexico.ui.board.ThemeDialog
import app.lexico.ui.board.Themed
import app.lexico.ui.classic.LocalShowUnseen
import app.lexico.ui.common.LocalConfettiStart

@Composable
fun App(lexico: Lexico, settings: Settings, version: String) {
  val cues = rememberCuePlayer(settings)
  val nav = remember(cues) { Navigator(cues::click) }
  val engine = rememberCreated(lexico) { lexico.start() }
  val menu = rememberMenuState()
  val themes = remember { ThemeState(settings) }
  BackHandler(enabled = nav.canGoBack, onBack = nav::back)
  Themed(themes.current) {
    Surface(Modifier.fillMaxSize()) {
      ModalNavigationDrawer(
        drawerState = menu.drawer,
        gesturesEnabled = !nav.current.isGame || menu.drawer.isOpen,
        drawerContent = { AppMenu(nav, menu, themes, cues::click) },
      ) {
        ScreenFrame(nav.current) { Screens(engine, lexico, nav, settings, themes, menu, cues) }
      }
      Dialogs(settings, themes, menu, version)
    }
  }
}

@Composable
private fun Dialogs(settings: Settings, themes: ThemeState, menu: MenuState, version: String) {
  if (menu.showOptions) OptionsDialog(settings) { menu.showOptions = false }
  if (menu.showAbout) AboutDialog(version) { menu.showAbout = false }
  if (themes.picking) ThemeDialog(themes.current, themes.lightBoard, themes::choose, themes::chooseLight) { themes.picking = false }
}

@Composable
private fun Screens(
  engine: Result<Unit>?, lexico: Lexico, nav: Navigator, settings: Settings, themes: ThemeState, menu: MenuState, cues: CuePlayer,
) {
  val scorer = remember(lexico) { engineScorer(lexico) }
  WhenReady(engine, "Cargando el motor…") {
    CompositionLocalProvider(
      LocalPlayScorer provides scorer.takeIf { settings.liveScore },
      LocalShowUnseen provides settings.showUnseen,
      LocalCues provides cues::play,
      LocalConfettiStart provides { cues.play(Cue.CELEBRATION) },
    ) {
      Content(nav.current, lexico, nav, settings, themes, onMenu = menu::open)
    }
  }
}

@Composable
private fun rememberCuePlayer(settings: Settings): CuePlayer {
  val ctx = LocalContext.current.applicationContext
  return remember(settings) { CuePlayer(ctx, settings) }
}

@Composable
private fun AppMenu(nav: Navigator, menu: MenuState, themes: ThemeState, click: () -> Unit) {
  SideMenu(
    atHome = nav.current == Screen.Home,
    onHome = { nav.home(); menu.close() },
    onStats = { nav.go(Screen.Stats); menu.close() },
    onTheme = { click(); themes.picking = true; menu.close() },
    onOptions = { click(); menu.showOptions = true; menu.close() },
    onAbout = { click(); menu.showAbout = true; menu.close() },
  )
}

@Composable
private fun ScreenFrame(screen: Screen, content: @Composable () -> Unit) {
  val margin = if (screen.hasBoard) 2.dp else 12.dp
  Column(Modifier.fillMaxSize().safeDrawingPadding().imePadding().padding(horizontal = margin, vertical = 6.dp)) { content() }
}
