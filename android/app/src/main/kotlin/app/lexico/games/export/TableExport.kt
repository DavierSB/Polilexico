package app.lexico.games.export

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import app.lexico.ui.board.BoardStyle
import app.lexico.ui.common.ExportDialog
import app.lexico.ui.common.ExportingDialog
import app.lexico.ui.duplicate.SeatView
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private sealed interface TableStep {
  data object Choosing : TableStep
  data object Working : TableStep
  data class Showing(val file: File, val format: ExportFormat) : TableStep
}

@Composable
fun TableExport(view: SeatView, style: BoardStyle, onDone: () -> Unit) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  var step by remember { mutableStateOf<TableStep>(TableStep.Choosing) }
  val render = { format: ExportFormat ->
    step = TableStep.Working
    scope.launch {
      runCatching { renderTable(context, view, style, format) }
        .onSuccess { step = TableStep.Showing(it, format) }
        .onFailure { report(context, Result.failure<Unit>(it)); onDone() }
    }
  }
  when (val s = step) {
    TableStep.Choosing -> ExportDialog(onJpg = { render(ExportFormat.JPG) }, onGif = { render(ExportFormat.GIF) }, onGcg = null, onClose = onDone)
    TableStep.Working -> ExportingDialog()
    is TableStep.Showing -> ExportPreview(s.file, s.format, { share(context, s.file, s.format) }, onDone)
  }
}

private suspend fun renderTable(context: Context, view: SeatView, style: BoardStyle, format: ExportFormat): File = withContext(Dispatchers.IO) {
  val dir = File(context.cacheDir, "exports").apply { mkdirs() }
  val file = File(dir, "lexico-duplicada-${System.currentTimeMillis()}.${format.extension}")
  file.outputStream().buffered().use {
    val gif = GameGif(context, style)
    if (format == ExportFormat.GIF) gif.write(tableStills(view), it) else gif.writeJpeg(finalStill(view), it)
  }
  file
}
