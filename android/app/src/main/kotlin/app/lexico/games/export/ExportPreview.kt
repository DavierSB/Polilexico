package app.lexico.games.export

import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.drawable.AnimatedImageDrawable
import android.os.Build
import android.widget.ImageView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.CreateDocument
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import java.io.File
import kotlinx.coroutines.launch

@Composable
internal fun ExportPreview(file: File, format: ExportFormat, onShare: () -> Unit, onClose: () -> Unit) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val save = rememberLauncherForActivityResult(CreateDocument(format.mime)) { uri ->
    uri?.let { scope.launch { report(context, runCatching { copyTo(context, file, it) }) } }
  }
  AlertDialog(
    onDismissRequest = onClose,
    text = { Picture(file, format) },
    confirmButton = {
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = { save.launch(file.name) }) { Text("Guardar") }
        Button(onClick = onShare) { Text("Compartir") }
      }
    },
    dismissButton = { TextButton(onClick = onClose) { Text("Cerrar") } },
  )
}

@Composable
private fun Picture(file: File, format: ExportFormat) {
  Column {
    AndroidView(
      factory = { ImageView(it).apply { adjustViewBounds = true; show(file, format) } },
      modifier = Modifier.fillMaxWidth().heightIn(max = 460.dp),
    )
  }
}

private fun ImageView.show(file: File, format: ExportFormat) {
  if (format == ExportFormat.GIF && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
    val drawable = ImageDecoder.decodeDrawable(ImageDecoder.createSource(file))
    setImageDrawable(drawable)
    (drawable as? AnimatedImageDrawable)?.start()
  } else setImageBitmap(BitmapFactory.decodeFile(file.path))
}
