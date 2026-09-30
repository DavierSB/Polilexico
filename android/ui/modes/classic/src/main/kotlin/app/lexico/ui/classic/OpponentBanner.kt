package app.lexico.ui.classic

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private const val BANNER_MS = 2200L

@Composable
fun OpponentBanner(moves: List<Move>, opponent: String) {
  rememberBannerText(moves, opponent)?.let { Banner(it) }
}

@Composable
private fun rememberBannerText(moves: List<Move>, opponent: String): String? {
  var text by remember { mutableStateOf<String?>(null) }
  var seen by remember { mutableIntStateOf(-1) }
  LaunchedEffect(moves.size) {
    val isNew = seen in 0 until moves.size
    seen = moves.size
    text = moves.lastOrNull()?.takeIf { isNew }?.let { bannerText(it, opponent) } ?: return@LaunchedEffect
    delay(BANNER_MS)
    text = null
  }
  return text
}

private fun bannerText(move: Move, opponent: String): String? = when {
  move.side != Side.OPPONENT -> null
  move.type == MoveType.PASS -> "$opponent pasó"
  move.type == MoveType.EXCHANGE -> "$opponent cambió ${move.tiles} fichas"
  else -> null
}

@Composable
private fun Banner(text: String) {
  Text(
    text, Modifier.background(MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp)).padding(horizontal = 22.dp, vertical = 12.dp),
    color = MaterialTheme.colorScheme.onPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold,
  )
}
