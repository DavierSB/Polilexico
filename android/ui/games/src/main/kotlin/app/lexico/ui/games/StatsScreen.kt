package app.lexico.ui.games

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.lexico.ui.common.BarTextButton
import app.lexico.ui.common.ConfirmDialog
import app.lexico.ui.common.Header

/**
 * Tus estadisticas, como en Woogles Offline: una pestaña por modalidad (en clasica, con un
 * selector de rival), la tabla de datos y las graficas. `pageFor` calcula lo que se ve para cada
 * filtro; `photo` dibuja la foto de un bot. "Reiniciar" hace que solo cuenten las partidas que
 * se terminen a partir de ahora.
 */
@Composable
fun StatsScreen(
  opponents: List<String>,
  photo: @Composable (String) -> Unit,
  pageFor: (StatsFilter) -> StatsPage,
  onReset: () -> Unit,
  onBack: () -> Unit,
) {
  var filter by remember { mutableStateOf(StatsFilter(StatsMode.CLASSIC)) }
  var confirmReset by remember { mutableStateOf(false) }
  Column(Modifier.fillMaxSize()) {
    Header("Mis estadísticas", onBack) { BarTextButton("Reiniciar") { confirmReset = true } }
    ModeTabs(filter.mode) { filter = StatsFilter(it) }
    StatsContent(filter, opponents, photo, remember(filter, pageFor) { pageFor(filter) }) { filter = filter.copy(opponent = it) }
  }
  if (confirmReset) ResetDialog(onReset = { onReset(); confirmReset = false }, onCancel = { confirmReset = false })
}

@Composable
private fun ModeTabs(mode: StatsMode, select: (StatsMode) -> Unit) {
  PrimaryTabRow(selectedTabIndex = mode.ordinal) {
    Tab(selected = mode == StatsMode.CLASSIC, onClick = { select(StatsMode.CLASSIC) }, text = { Text("Clásica") })
    Tab(selected = mode == StatsMode.DUPLICATE, onClick = { select(StatsMode.DUPLICATE) }, text = { Text("Duplicada") })
  }
}

@Composable
private fun StatsContent(
  filter: StatsFilter, opponents: List<String>, photo: @Composable (String) -> Unit, page: StatsPage, selectOpponent: (String?) -> Unit,
) {
  Column(Modifier.verticalScroll(rememberScrollState()).padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
    if (filter.mode == StatsMode.CLASSIC) OpponentSelector(filter.opponent, opponents, photo, selectOpponent)
    if (page.rows.isEmpty()) return@Column Text(page.emptyText, style = MaterialTheme.typography.bodyMedium)
    StatsTable(page.rows)
    page.note?.let { Note(it) }
    Charts(filter.mode, page.games)
  }
}

/** En clasica: tus puntos, o tus puntos contra los del rival. En duplicada: la eficiencia. */
@Composable
private fun ColumnScope.Charts(mode: StatsMode, games: List<ChartGame>) {
  if (mode == StatsMode.CLASSIC) ClassicCharts(games) else DuplicateChart(games)
  Note("Toca un punto para ver esa partida.")
}

@Composable
private fun ClassicCharts(games: List<ChartGame>) {
  var scatter by remember { mutableStateOf(false) }
  ChartSwitch(scatter) { scatter = it }
  if (scatter) ScoreScatter(games) else DotStrip(games, "Tus puntos")
}

@Composable
private fun DuplicateChart(games: List<ChartGame>) {
  Text("Efectividad", style = MaterialTheme.typography.titleSmall)
  DotStrip(games, "Efectividad por partida (% de los puntos del máster)", unit = "%")
}

@Composable
private fun Note(text: String) {
  Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun ResetDialog(onReset: () -> Unit, onCancel: () -> Unit) {
  ConfirmDialog(
    "¿Reiniciar las estadísticas? Tus partidas no se borran (siguen en «Mis partidas»), pero solo contarán las que termines a partir de ahora.",
    yes = "Reiniciar", no = "Cancelar", onYes = onReset, onNo = onCancel,
  )
}
