package app.lexico.ui.lists

import android.content.Context
import android.net.Uri
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.lexico.ui.common.Header

private const val RAE_URL = "https://dle.rae.es/"

@Composable
internal fun RaeDialog(word: String, onClose: () -> Unit) {
  Dialog(onClose, DialogProperties(usePlatformDefaultWidth = false)) {
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
      Column {
        Header("RAE · " + WordKeys.upper(word), onClose)
        AndroidView({ raeView(it, word) }, Modifier.fillMaxWidth().weight(1f), onRelease = WebView::destroy)
      }
    }
  }
}

private fun raeView(context: Context, word: String): WebView = WebView(context).apply {
  settings.javaScriptEnabled = true
  settings.domStorageEnabled = true
  settings.cacheMode = WebSettings.LOAD_DEFAULT
  webViewClient = OfflineFallback()
  loadUrl(RAE_URL + Uri.encode(WordKeys.show(word)))
}

private class OfflineFallback : WebViewClient() {
  private var failures = 0

  override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
    if (!request.isForMainFrame) return
    failures++
    if (failures == 1) fromCache(view, request.url.toString()) else if (failures == 2) noConnection(view)
  }

  private fun fromCache(view: WebView, url: String) {
    view.settings.cacheMode = WebSettings.LOAD_CACHE_ONLY
    view.loadUrl(url)
  }

  private fun noConnection(view: WebView) {
    view.loadDataWithBaseURL(null, OFFLINE_PAGE, "text/html", "utf-8", null)
  }

  private companion object {
    const val OFFLINE_PAGE = "<html><body style='font-family:sans-serif;padding:24px;text-align:center'>" +
      "<p>Sin conexión, y esta palabra no se ha consultado antes en la RAE.</p></body></html>"
  }
}
