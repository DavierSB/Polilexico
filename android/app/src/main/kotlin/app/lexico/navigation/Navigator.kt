package app.lexico.navigation

import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateListOf

@Stable
class Navigator(private val onMove: () -> Unit = {}) {
  private val stack = mutableStateListOf<Screen>(Screen.Home)

  val current: Screen get() = stack.last()
  val canGoBack: Boolean get() = stack.size > 1

  fun go(screen: Screen) {
    onMove()
    stack += screen
  }

  fun replace(screen: Screen) {
    onMove()
    stack[stack.lastIndex] = screen
  }

  fun back() {
    if (!canGoBack) return
    onMove()
    stack.removeAt(stack.lastIndex)
  }

  fun home() {
    onMove()
    stack.clear()
    stack += Screen.Home
  }
}
