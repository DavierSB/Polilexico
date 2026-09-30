package app.lexico.ui.recall

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.lexico.model.Board
import app.lexico.ui.board.BoardStyle
import app.lexico.ui.board.ScrabbleBoard
import app.lexico.ui.common.Mulish
import kotlinx.coroutines.delay

private const val FINAL_PAUSE_MS = 2000L

@Composable
internal fun Watching(game: List<ScoredPlacement>, intervalMs: Long, style: BoardStyle, onDone: () -> Unit) {
  val replay = remember(game) { Replay() }
  LaunchedEffect(game) {
    replay.play(game, intervalMs)
    delay(FINAL_PAUSE_MS)
    onDone()
  }
  WatchingHeading(replay.shown, game.size)
  ScrabbleBoard(replay.board, Modifier.fillMaxWidth(), style, latestScore = replay.latestScore, showScore = true)
  Text(
    "Fíjate en las palabras que más puntos hacen: luego tendrás que armarlas.",
    Modifier.fillMaxWidth().padding(horizontal = 8.dp), style = MaterialTheme.typography.bodyMedium,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
  )
}

@Composable
private fun WatchingHeading(shown: Int, total: Int) {
  Column(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
    Row(verticalAlignment = Alignment.Bottom) {
      Text("MEMORIZA", Modifier.weight(1f), fontFamily = Mulish, fontWeight = FontWeight.Black, fontSize = 13.sp,
        letterSpacing = 2.sp, color = MaterialTheme.colorScheme.primary)
      Text("$shown", fontFamily = Mulish, fontWeight = FontWeight.Black, fontSize = 22.sp)
      Text(" / $total", fontFamily = Mulish, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    LinearProgressIndicator(
      progress = { if (total == 0) 0f else shown.toFloat() / total },
      Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)), gapSize = 0.dp, drawStopIndicator = {},
    )
  }
}

@Stable
private class Replay {
  var board by mutableStateOf(Board.EMPTY)
    private set
  var shown by mutableIntStateOf(0)
    private set
  var latestScore: Int? by mutableStateOf(null)
    private set

  suspend fun play(game: List<ScoredPlacement>, intervalMs: Long) {
    for ((placement, score) in game) {
      delay(intervalMs)
      board = runCatching { board.play(placement) }.getOrDefault(board)
      latestScore = score
      shown++
    }
  }
}
