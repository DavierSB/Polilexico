package app.lexico.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun <T> MovesTableDialog(
  rows: List<T>, header: @Composable () -> Unit, close: () -> Unit, footer: (@Composable () -> Unit)? = null,
  row: @Composable (Int, T) -> Unit,
) {
  Dialog(onDismissRequest = close, properties = DialogProperties(usePlatformDefaultWidth = false)) {
    Surface(Modifier.fillMaxWidth().padding(horizontal = 8.dp), shape = AlertDialogDefaults.shape, color = AlertDialogDefaults.containerColor) {
      Column(Modifier.padding(start = 12.dp, end = 12.dp, top = 20.dp, bottom = 8.dp)) {
        Text("Movidas", Modifier.padding(start = 4.dp, bottom = 12.dp), style = MaterialTheme.typography.headlineSmall)
        if (rows.isEmpty()) Text("Todavía no hay movidas.") else MovesTable(rows, header, footer, row)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { TextButton(onClick = close) { Text("Cerrar") } }
      }
    }
  }
}

@Composable
fun moveNumberWidth(): Dp = with(LocalDensity.current) { 18.sp.toDp() }

@Composable
fun MoveText(text: String, modifier: Modifier = Modifier) {
  Text(
    text, modifier, fontFamily = FontFamily.Monospace, maxLines = 1, softWrap = false,
    autoSize = TextAutoSize.StepBased(minFontSize = 7.sp, maxFontSize = 12.sp, stepSize = 0.5.sp),
  )
}

@Composable
private fun <T> MovesTable(rows: List<T>, header: @Composable () -> Unit, footer: (@Composable () -> Unit)?, row: @Composable (Int, T) -> Unit) {
  val list = rememberLazyListState()
  val last = if (footer == null) rows.size - 1 else rows.size
  LaunchedEffect(last) { list.scrollToItem(last) }
  Column {
    header()
    HorizontalDivider()
    LazyColumn(Modifier.heightIn(max = 420.dp), state = list) {
      itemsIndexed(rows) { i, r -> row(i, r) }
      footer?.let { item { it() } }
    }
  }
}
