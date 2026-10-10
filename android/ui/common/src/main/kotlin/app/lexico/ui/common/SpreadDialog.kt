package app.lexico.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp

@Composable
fun SpreadDialog(spread: Spread, close: () -> Unit, selected: Int? = null, pick: ((Int) -> Unit)? = null) {
  ChartDialog("Ventaja", close) { SpreadChart(spread, selected = selected, pick = pick, tapped = close) }
}

@Composable
fun EfficiencyDialog(efficiency: Efficiency, close: () -> Unit, selected: Int? = null, pick: ((Int) -> Unit)? = null) {
  var view by remember { mutableStateOf(EfficiencyView.RUNNING) }
  ChartDialog("Eficiencia", close) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      EfficiencySwitch(view) { view = it }
      EfficiencyChart(efficiency, view, selected = selected, pick = pick, tapped = close)
    }
  }
}

@Composable
private fun EfficiencySwitch(view: EfficiencyView, select: (EfficiencyView) -> Unit) {
  Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
    FilterChip(view == EfficiencyView.RUNNING, { select(EfficiencyView.RUNNING) }, { Text("Acumulada", maxLines = 1) })
    FilterChip(view == EfficiencyView.PER_TURN, { select(EfficiencyView.PER_TURN) }, { Text("Jugada a jugada", maxLines = 1) })
  }
}

@Composable
private fun ChartDialog(title: String, close: () -> Unit, chart: @Composable () -> Unit) {
  AlertDialog(
    onDismissRequest = close,
    title = { Text(title) },
    text = chart,
    confirmButton = { TextButton(onClick = close) { Text("Cerrar") } },
  )
}
