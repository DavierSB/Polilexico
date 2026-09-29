package app.lexico.navigation

import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateListOf

/**
 * La pila de pantallas: [go] apila, [back] desapila. Empezar una partida reemplaza su
 * pantalla de opciones, asi "volver" desde la partida lleva al inicio y no a las opciones.
 */
@Stable
class Navigator {
  private val stack = mutableStateListOf<Screen>(Screen.Home)

  val current: Screen get() = stack.last()
  val canGoBack: Boolean get() = stack.size > 1

  fun go(screen: Screen) {
    stack += screen
  }

  /** Cambia la pantalla actual por `screen` (de las opciones a la partida). */
  fun replace(screen: Screen) {
    stack[stack.lastIndex] = screen
  }

  fun back() {
    if (canGoBack) stack.removeAt(stack.lastIndex)
  }

  fun home() {
    stack.clear()
    stack += Screen.Home
  }
}
