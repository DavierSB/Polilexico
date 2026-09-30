package app.lexico.menu

import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun rememberMenuState(): MenuState {
  val drawer = rememberDrawerState(DrawerValue.Closed)
  val scope = rememberCoroutineScope()
  return remember { MenuState(drawer, scope) }
}

@Stable
class MenuState(val drawer: DrawerState, private val scope: CoroutineScope) {
  var showOptions by mutableStateOf(false)

  var showAbout by mutableStateOf(false)

  fun open() {
    scope.launch { drawer.open() }
  }

  fun close() {
    scope.launch { drawer.close() }
  }
}
