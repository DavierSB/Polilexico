package app.lexico.ui.lists

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

class StudyPlayer(private val context: Context) {
  var playing by mutableStateOf(false)
  var seconds by mutableIntStateOf(saved())
    private set
  fun nextPace() {
    seconds = PACES[(PACES.indexOf(seconds) + 1) % PACES.size]
    prefs().edit().putInt(KEY, seconds).apply()
  }

  private fun saved(): Int = prefs().getInt(KEY, DEFAULT).takeIf { it in PACES } ?: DEFAULT

  private fun prefs() = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

  private companion object {
    val PACES = listOf(2, 3, 5, 8)
    const val DEFAULT = 3
    const val PREFS = "study"
    const val KEY = "seconds"
  }
}

@Composable
fun rememberPlayer(context: Context): StudyPlayer = remember { StudyPlayer(context.applicationContext) }
