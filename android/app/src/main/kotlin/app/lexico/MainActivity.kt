package app.lexico

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import app.lexico.game.Lexico
import app.lexico.menu.Settings
import app.lexico.navigation.App

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    enableEdgeToEdge()
    super.onCreate(savedInstanceState)
    val lexico = Lexico(applicationContext)
    val settings = Settings(applicationContext)
    val version = packageManager.getPackageInfo(packageName, 0).versionName.orEmpty()
    setContent { App(lexico, settings, version) }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) window.decorView.isForceDarkAllowed = false
  }
}
