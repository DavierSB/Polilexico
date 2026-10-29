package app.lexico.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.lexico.game.Lexico
import app.lexico.modes.lists.WordLexicon
import app.lexico.ui.common.LocalHomeButton
import app.lexico.ui.common.LocalWordLookup
import app.lexico.ui.lists.Lexicon
import app.lexico.ui.lists.WordKeys
import app.lexico.ui.lists.WordScreen
import app.lexico.ui.lists.WordSearchScreen

@Composable
internal fun WordSearchRoute(lexico: Lexico, nav: Navigator) {
  WithWords(lexico) { lexicon -> WordSearchScreen(lexicon, nav::back) }
}

@Composable
internal fun WordLookupHost(lexico: Lexico, onMove: () -> Unit, content: @Composable () -> Unit) {
  var entry by rememberSaveable { mutableStateOf<String?>(null) }
  val open = remember(onMove) { { text: String -> onMove(); entry = WordKeys.entry(text) } }
  CompositionLocalProvider(LocalWordLookup provides open) {
    content()
    entry?.let { shown -> WordDialog(shown, lexico, open) { onMove(); entry = null } }
  }
}

@Composable
private fun WordDialog(entry: String, lexico: Lexico, onOpen: (String) -> Unit, onClose: () -> Unit) {
  Dialog(onClose, DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
    Surface(Modifier.fillMaxSize()) {
      CompositionLocalProvider(LocalHomeButton provides null) {
        Column(Modifier.safeDrawingPadding().padding(horizontal = 12.dp, vertical = 6.dp)) {
          WithWords(lexico) { lexicon -> WordScreen(entry, lexicon, onClose, onOpen) }
        }
      }
    }
  }
}

@Composable
internal fun WithWords(lexico: Lexico, content: @Composable (Lexicon) -> Unit) {
  val ready = WordLexicon.ready
  if (ready != null) return content(ready)
  WhenReady(rememberCreated(Unit) { WordLexicon.warm(lexico) }, "Cargando las palabras…", content)
}
