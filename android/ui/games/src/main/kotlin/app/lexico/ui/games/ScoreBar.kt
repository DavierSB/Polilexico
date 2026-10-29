package app.lexico.ui.games

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import app.lexico.ui.board.BoardStyle
import app.lexico.ui.classic.OpponentPhoto
import app.lexico.ui.classic.OpponentRack
import app.lexico.ui.classic.PlayerBar

private val BAR_TILE = 20.dp
private val BAR_PHOTO = 20.dp

@Composable
internal fun ScoreBar(review: ReviewView, name: String, points: Int, rack: List<String>?, onTurn: Boolean, style: BoardStyle) {
  val photo: (@Composable () -> Unit)? = if (name != ME && review.photo) ({ OpponentPhoto(review.opponent, BAR_PHOTO) }) else null
  PlayerBar(name = name, points = points, clock = null, onTurn = onTurn, framed = true, photo = photo, tilesBeside = true) {
    rack?.let { OpponentRack(OpponentRack.Visible(it), style, BAR_TILE) }
  }
}
