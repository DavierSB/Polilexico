package app.lexico.ui.games

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

/** Que grafica se ve en clasica: tus puntos, o tus puntos contra los del rival. */
@Composable
internal fun ChartSwitch(scatter: Boolean, select: (scatter: Boolean) -> Unit) {
  Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
    FilterChip(selected = !scatter, onClick = { select(false) }, label = { Text("Tus puntos") })
    FilterChip(selected = scatter, onClick = { select(true) }, label = { Text("Tú vs rival") })
  }
}
