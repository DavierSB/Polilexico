package app.lexico.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.unit.dp

/** Cuanto se tapa la partida en pausa (el desenfoque solo existe desde Android 12). */
private const val COVER_ALPHA = 0.92f

/**
 * Una partida que se puede pausar: en pausa, el contenido se desenfoca y queda tapado por
 * "Juego pausado" y el boton para continuar. Tapar el tablero y el atril evita que la pausa
 * sirva para pensar sin gastar tiempo.
 */
@Composable
fun Pausable(paused: Boolean, onResume: () -> Unit, content: @Composable () -> Unit) {
  Box {
    Box(if (paused) Modifier.blur(16.dp) else Modifier) { content() }
    if (paused) PauseCover(onResume)
  }
}

/** Tapa todo el contenido y se queda con los toques, salvo el boton de continuar. */
@Composable
private fun BoxScope.PauseCover(onResume: () -> Unit) {
  Column(
    Modifier.matchParentSize().background(MaterialTheme.colorScheme.surface.copy(alpha = COVER_ALPHA)).swallowTaps(),
    verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Text("Juego pausado", style = MaterialTheme.typography.headlineSmall)
    Button(onClick = onResume) { Text("Continuar") }
  }
}

@Composable
private fun Modifier.swallowTaps(): Modifier =
  clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
