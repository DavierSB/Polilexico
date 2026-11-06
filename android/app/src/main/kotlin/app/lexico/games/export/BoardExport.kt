package app.lexico.games.export

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import app.lexico.model.Board
import app.lexico.ui.board.BoardStyle
import app.lexico.ui.common.ExportingDialog
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun BoardExport(board: Board, title: String, caption: String, style: BoardStyle, onDone: () -> Unit) {
  val context = LocalContext.current
  var file by remember { mutableStateOf<File?>(null) }
  LaunchedEffect(board) {
    runCatching { renderBoard(context, Still(board, title, caption), style) }
      .onSuccess { file = it }
      .onFailure { report(context, Result.failure<Unit>(it)); onDone() }
  }
  val shown = file ?: return ExportingDialog()
  ExportPreview(shown, ExportFormat.JPG, { share(context, shown, ExportFormat.JPG) }, onDone)
}

private suspend fun renderBoard(context: Context, still: Still, style: BoardStyle): File = withContext(Dispatchers.IO) {
  val dir = File(context.cacheDir, "exports").apply { mkdirs() }
  val file = File(dir, "lexico-duplicada-${System.currentTimeMillis()}.jpg")
  file.outputStream().buffered().use { GameGif(context, style).writeJpeg(still, it) }
  file
}
