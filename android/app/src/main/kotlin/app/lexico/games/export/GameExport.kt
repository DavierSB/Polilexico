package app.lexico.games.export

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.CreateDocument
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import app.lexico.game.records.FinishedGames
import app.lexico.ui.board.BoardStyle
import app.lexico.ui.common.ExportDialog
import app.lexico.ui.common.ExportKind
import app.lexico.ui.common.ExportingDialog
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val LOG_SUFFIX = "-log.json"

private enum class ExportFormat(val extension: String, val mime: String) {
  GCG("gcg", "application/octet-stream"),
  GIF("gif", "image/gif"),
}

private enum class ExportStep { CHOOSING, PICKING, WRITING }

@Composable
fun GameExport(recordPath: String, games: FinishedGames, style: BoardStyle, start: ExportKind? = null, onDone: () -> Unit) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  var step by remember { mutableStateOf(ExportStep.CHOOSING) }
  val save = { format: ExportFormat, uri: Uri? ->
    if (uri == null) onDone() else {
      step = ExportStep.WRITING
      scope.launch { report(context, runCatching { write(context, uri, Export(recordPath, format, games, style)) }); onDone() }
    }
  }
  val gcg = rememberLauncherForActivityResult(CreateDocument(ExportFormat.GCG.mime)) { save(ExportFormat.GCG, it) }
  val gif = rememberLauncherForActivityResult(CreateDocument(ExportFormat.GIF.mime)) { save(ExportFormat.GIF, it) }
  LaunchedEffect(start) {
    when (start) {
      ExportKind.GCG -> { step = ExportStep.PICKING; gcg.launch(fileName(recordPath, ExportFormat.GCG)) }
      ExportKind.GIF -> { step = ExportStep.PICKING; gif.launch(fileName(recordPath, ExportFormat.GIF)) }
      null -> Unit
    }
  }
  when (step) {
    ExportStep.CHOOSING -> if (start == null) ExportDialog(
      onGcg = { step = ExportStep.PICKING; gcg.launch(fileName(recordPath, ExportFormat.GCG)) },
      onGif = { step = ExportStep.PICKING; gif.launch(fileName(recordPath, ExportFormat.GIF)) },
      onClose = onDone,
    )
    ExportStep.PICKING -> Unit
    ExportStep.WRITING -> ExportingDialog()
  }
}

private class Export(val recordPath: String, val format: ExportFormat, val games: FinishedGames, val style: BoardStyle)

private suspend fun write(context: Context, uri: Uri, export: Export) = withContext(Dispatchers.IO) {
  val out = requireNotNull(context.contentResolver.openOutputStream(uri)) { "no se pudo abrir $uri" }
  out.buffered().use {
    when (export.format) {
      ExportFormat.GCG -> it.write(withBotAliases(gcgFile(export.recordPath).readText()).toByteArray())
      ExportFormat.GIF -> GameGif(context, export.style).write(export.games.open(export.recordPath), it)
    }
  }
}

private fun report(context: Context, result: Result<*>) {
  val text = if (result.isSuccess) "Partida exportada." else "No se pudo exportar la partida."
  Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
}

private fun gcgFile(recordPath: String): File = File(recordPath.removeSuffix(LOG_SUFFIX) + ".gcg")

private fun fileName(recordPath: String, format: ExportFormat): String =
  "lexico-" + File(recordPath).name.removeSuffix(LOG_SUFFIX) + "." + format.extension
