package app.lexico.ui.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.lexico.model.Letters

interface PhotoSource {
  @Composable
  fun of(name: String, country: String): ImageBitmap?

  @Composable
  fun ofSeat(seatName: String): ImageBitmap?
}

val LocalPhotos = compositionLocalOf<PhotoSource?> { null }

@Composable
fun PersonAvatar(name: String, country: String, highlighted: Boolean = false, size: Dp = 40.dp) {
  PlayerAvatar(name, highlighted, size, LocalPhotos.current?.of(name, country))
}

@Composable
fun SeatAvatar(seatName: String, size: Dp) {
  PlayerAvatar(seatName, highlighted = false, size, LocalPhotos.current?.ofSeat(seatName))
}

@Composable
internal fun InitialTile(name: String, highlighted: Boolean, size: Dp) {
  val shape = RoundedCornerShape(size * 0.18f)
  val edge = if (highlighted) MaterialTheme.colorScheme.primary else TILE_EDGE
  val letter = initialOf(name)
  Box(
    Modifier.size(size).background(TILE_FACE, shape).border(if (highlighted) 2.dp else 1.dp, edge, shape),
    contentAlignment = Alignment.Center,
  ) {
    Text(letter, fontSize = (size.value * if (letter.length > 1) 0.38f else 0.55f).sp, fontWeight = FontWeight.Black, color = TILE_INK, softWrap = false)
    TileValue(Letters.value(letter), size)
  }
}

@Composable
private fun BoxScope.TileValue(value: Int, size: Dp) {
  if (value <= 0) return
  Text(
    "$value", Modifier.align(Alignment.BottomEnd).padding(end = size * 0.08f, bottom = size * 0.02f),
    fontSize = (size.value * 0.22f).sp, fontWeight = FontWeight.Bold, color = TILE_INK, softWrap = false,
  )
}

private fun initialOf(name: String): String {
  val upper = name.trim().uppercase()
  return upper.take(2).takeIf { it in NAME_DIGRAPHS } ?: upper.take(1)
}

private val NAME_DIGRAPHS = setOf("CH", "LL")

private val TILE_FACE = Color(0xFFF2DCA8)
private val TILE_EDGE = Color(0xFFC9A96A)
private val TILE_INK = Color(0xFF3B2A1A)

@Composable
internal fun PhotoCircle(image: ImageBitmap, description: String, size: Dp) {
  Image(image, description, Modifier.size(size).clip(CircleShape), contentScale = ContentScale.Crop)
}
