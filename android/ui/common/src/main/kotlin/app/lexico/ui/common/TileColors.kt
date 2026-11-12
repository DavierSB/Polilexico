package app.lexico.ui.common

import androidx.compose.ui.graphics.Color
import kotlin.random.Random

internal data class TileColors(val face: Color, val edge: Color, val ink: Color)

internal fun tileColorsOf(name: String): TileColors =
  TILE_PALETTE[Random(name.trim().lowercase().hashCode() xor SESSION_SEED).nextInt(TILE_PALETTE.size)]

private val SESSION_SEED = Random.nextInt()

private val TILE_PALETTE = listOf(
  tileColors(0xFFF2DCA8, 0xFFC9A96A, 0xFF3B2A1A),
  tileColors(0xFFF4B6A6, 0xFFD2826F, 0xFF4A1E14),
  tileColors(0xFFB9E3CF, 0xFF7FB89E, 0xFF173A2C),
  tileColors(0xFFB8D6F2, 0xFF7FA6CC, 0xFF142C45),
  tileColors(0xFFD4C6EE, 0xFFA08FC8, 0xFF2A1D47),
  tileColors(0xFFF2C2D8, 0xFFCC8AA8, 0xFF45182D),
  tileColors(0xFFF1E58E, 0xFFC8B850, 0xFF3A3310),
  tileColors(0xFF5B6B7A, 0xFF3E4B57, 0xFFF4F1EA),
  tileColors(0xFF8E3B4A, 0xFF6A2834, 0xFFFBEDEF),
  tileColors(0xFF3F6B55, 0xFF2B4D3C, 0xFFEEF6F1),
)

private fun tileColors(face: Long, edge: Long, ink: Long) = TileColors(Color(face), Color(edge), Color(ink))
