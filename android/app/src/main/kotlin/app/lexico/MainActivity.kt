package app.lexico

import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import app.lexico.game.Lexico
import app.lexico.menu.Settings
import app.lexico.navigation.App
import app.lexico.update.Updates

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    enableEdgeToEdge()
    super.onCreate(savedInstanceState)
    volumeControlStream = AudioManager.STREAM_MUSIC
    val lexico = Lexico(applicationContext)
    val settings = Settings(applicationContext)
    val version = packageManager.getPackageInfo(packageName, 0).versionName.orEmpty()
    val updates = Updates(applicationContext, version)
    setContent { App(lexico, settings, updates, version) }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) window.decorView.isForceDarkAllowed = false
  }
}
