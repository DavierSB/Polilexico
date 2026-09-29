package app.lexico.ui.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * La planilla de movidas de una partida: una cabecera y una fila por turno, abierta en el ultimo.
 * Cada modalidad pone sus columnas en `header` y `row` (con el indice del turno, desde 0).
 */
@Composable
fun <T> MovesTableDialog(rows: List<T>, header: @Composable () -> Unit, close: () -> Unit, row: @Composable (Int, T) -> Unit) {
  AlertDialog(
    onDismissRequest = close,
    title = { Text("Movidas") },
    text = { if (rows.isEmpty()) Text("Todavía no hay movidas.") else MovesTable(rows, header, row) },
    confirmButton = { TextButton(onClick = close) { Text("Cerrar") } },
  )
}

@Composable
private fun <T> MovesTable(rows: List<T>, header: @Composable () -> Unit, row: @Composable (Int, T) -> Unit) {
  val list = rememberLazyListState()
  LaunchedEffect(rows.size) { list.scrollToItem(rows.size - 1) }
  Column {
    header()
    HorizontalDivider()
    LazyColumn(Modifier.heightIn(max = 420.dp), state = list) { itemsIndexed(rows) { i, r -> row(i, r) } }
  }
}
