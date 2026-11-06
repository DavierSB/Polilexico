package app.lexico.games.export

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.CreateDocument
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import app.lexico.game.records.FinishedGames
import app.lexico.ui.board.BoardStyle
import app.lexico.ui.common.ExportDialog
import app.lexico.ui.common.ExportingDialog
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val LOG_SUFFIX = "-log.json"
private const val EXPORTS = "exports"

internal enum class ExportFormat(val extension: String, val mime: String) {
  GCG("gcg", "application/octet-stream"),
  GIF("gif", "image/gif"),
  JPG("jpg", "image/jpeg"),
}

private sealed interface ExportStep {
  data object Choosing : ExportStep
  data object Working : ExportStep
  data class Showing(val file: File, val format: ExportFormat) : ExportStep
}

@Composable
fun GameExport(recordPath: String, games: FinishedGames, style: BoardStyle, onDone: () -> Unit) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  var step by remember { mutableStateOf<ExportStep>(ExportStep.Choosing) }
  val export = Export(recordPath, games, style)
  val gcg = rememberLauncherForActivityResult(CreateDocument(ExportFormat.GCG.mime)) { uri ->
    uri?.let { scope.launch { report(context, runCatching { writeGcg(context, it, export) }) } }
    onDone()
  }
  val render = { format: ExportFormat ->
    step = ExportStep.Working
    scope.launch {
      runCatching { render(context, export, format) }
        .onSuccess { step = ExportStep.Showing(it, format) }
        .onFailure { report(context, it); onDone() }
    }
  }
  when (val s = step) {
    ExportStep.Choosing -> ExportDialog(
      onJpg = { render(ExportFormat.JPG) }, onGif = { render(ExportFormat.GIF) },
      onGcg = { step = ExportStep.Working; gcg.launch(fileName(recordPath, ExportFormat.GCG)) }, onClose = onDone,
    )
    ExportStep.Working -> ExportingDialog()
    is ExportStep.Showing -> ExportPreview(s.file, s.format, { share(context, s.file, s.format) }, onDone)
  }
}

internal class Export(val recordPath: String, val games: FinishedGames, val style: BoardStyle)

private suspend fun render(context: Context, export: Export, format: ExportFormat): File = withContext(Dispatchers.IO) {
  val dir = File(context.cacheDir, EXPORTS).apply { mkdirs() }
  val file = File(dir, fileName(export.recordPath, format))
  val review = export.games.open(export.recordPath)
  file.outputStream().buffered().use {
    if (format == ExportFormat.GIF) GameGif(context, export.style).write(review, it) else GameGif(context, export.style).writeJpeg(review, it)
  }
  file
}

private suspend fun writeGcg(context: Context, uri: Uri, export: Export) = withContext(Dispatchers.IO) {
  val out = requireNotNull(context.contentResolver.openOutputStream(uri)) { "no se pudo abrir $uri" }
  out.buffered().use { it.write(withBotAliases(gcgFile(export.recordPath).readText()).toByteArray()) }
}

internal fun share(context: Context, file: File, format: ExportFormat) {
  val uri = FileProvider.getUriForFile(context, context.packageName + ".updates", file)
  val send = Intent(Intent.ACTION_SEND).setType(format.mime).putExtra(Intent.EXTRA_STREAM, uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
  context.startActivity(Intent.createChooser(send, "Compartir partida"))
}

internal suspend fun copyTo(context: Context, file: File, uri: Uri) = withContext(Dispatchers.IO) {
  val out = requireNotNull(context.contentResolver.openOutputStream(uri)) { "no se pudo abrir $uri" }
  out.buffered().use { file.inputStream().use { input -> input.copyTo(it) } }
}

internal fun report(context: Context, result: Result<*>) {
  val text = if (result.isSuccess) "Partida exportada." else "No se pudo exportar la partida."
  Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
}

private fun report(context: Context, error: Throwable) = report(context, Result.failure<Unit>(error))

private fun gcgFile(recordPath: String): File = File(recordPath.removeSuffix(LOG_SUFFIX) + ".gcg")

internal fun fileName(recordPath: String, format: ExportFormat): String =
  "lexico-" + File(recordPath).name.removeSuffix(LOG_SUFFIX) + "." + format.extension
