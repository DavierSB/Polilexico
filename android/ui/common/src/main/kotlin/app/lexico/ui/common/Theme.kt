package app.lexico.ui.common

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

val PolimitaColors: ColorScheme = palette(
  primary = 0xFFFFB631, onPrimary = 0xFF2A140C, secondary = 0xFFF2831E, tertiary = 0xFFE8C07A,
  background = 0xFF1B1411, surfaceVariant = 0xFF3A2C24, onSurface = 0xFFFFF1D6, onSurfaceVariant = 0xFFD9C4B0,
  outline = 0xFF9E8A7A, outlineVariant = 0xFF4F4037, primaryContainer = 0xFF5A3A12, onPrimaryContainer = 0xFFFFDDA6,
  secondaryContainer = 0xFF4A2C1A, onSecondaryContainer = 0xFFFFDCC2,
  containers = listOf(0xFF140E0B, 0xFF231A16, 0xFF281E19, 0xFF32271F, 0xFF3D3029),
)

val LeafColors: ColorScheme = palette(
  primary = 0xFF8FD46A, onPrimary = 0xFF10200C, secondary = 0xFFF07AA8, tertiary = 0xFFD8E36A,
  background = 0xFF0E140D, surfaceVariant = 0xFF263323, onSurface = 0xFFEEF4EA, onSurfaceVariant = 0xFFC3CFB8,
  outline = 0xFF8A9A80, outlineVariant = 0xFF364433, primaryContainer = 0xFF2E5A1E, onPrimaryContainer = 0xFFD2F0B8,
  secondaryContainer = 0xFF5A1E38, onSecondaryContainer = 0xFFFFD6E6,
  containers = listOf(0xFF090F08, 0xFF151D14, 0xFF192218, 0xFF212B1F, 0xFF2A3528),
)

val SkyColors: ColorScheme = palette(
  primary = 0xFF74ACDF, onPrimary = 0xFF0B1E33, secondary = 0xFFF6B40E, tertiary = 0xFFB9D6F0,
  background = 0xFF0F1620, surfaceVariant = 0xFF24303D, onSurface = 0xFFE8F0F8, onSurfaceVariant = 0xFFBFCBD8,
  outline = 0xFF8595A6, outlineVariant = 0xFF35414F, primaryContainer = 0xFF1F4466, onPrimaryContainer = 0xFFCDE5FA,
  secondaryContainer = 0xFF4A3A0C, onSecondaryContainer = 0xFFFFE3A0,
  containers = listOf(0xFF0A1018, 0xFF151D28, 0xFF19222E, 0xFF212B38, 0xFF2A3543),
)

val NightColors: ColorScheme = palette(
  primary = 0xFF7FA6E8, onPrimary = 0xFF0A1A33, secondary = 0xFFE8D9B5, tertiary = 0xFFA9B8D6,
  background = 0xFF10141C, surfaceVariant = 0xFF262C38, onSurface = 0xFFE4E8F0, onSurfaceVariant = 0xFFBCC3D0,
  outline = 0xFF8A93A3, outlineVariant = 0xFF353C4A, primaryContainer = 0xFF263F6B, onPrimaryContainer = 0xFFD6E3FF,
  secondaryContainer = 0xFF45402F, onSecondaryContainer = 0xFFF5ECD4,
  containers = listOf(0xFF0B0E14, 0xFF161B24, 0xFF1A2029, 0xFF222833, 0xFF2B323E),
)

@Composable
fun LexicoTheme(colors: ColorScheme = PolimitaColors, content: @Composable () -> Unit) {
  MaterialTheme(colorScheme = colors, content = content)
}

val Compact = PaddingValues(horizontal = 6.dp, vertical = 2.dp)

private fun palette(
  primary: Long, onPrimary: Long, secondary: Long, tertiary: Long, background: Long, surfaceVariant: Long,
  onSurface: Long, onSurfaceVariant: Long, outline: Long, outlineVariant: Long, primaryContainer: Long,
  onPrimaryContainer: Long, secondaryContainer: Long, onSecondaryContainer: Long, containers: List<Long>,
): ColorScheme = darkColorScheme(
  primary = Color(primary), onPrimary = Color(onPrimary),
  primaryContainer = Color(primaryContainer), onPrimaryContainer = Color(onPrimaryContainer),
  secondary = Color(secondary), onSecondary = Color(onPrimary),
  secondaryContainer = Color(secondaryContainer), onSecondaryContainer = Color(onSecondaryContainer),
  tertiary = Color(tertiary), onTertiary = Color(onPrimary),
  background = Color(background), onBackground = Color(onSurface),
  surface = Color(background), onSurface = Color(onSurface),
  surfaceVariant = Color(surfaceVariant), onSurfaceVariant = Color(onSurfaceVariant),
  surfaceContainerLowest = Color(containers[0]), surfaceContainerLow = Color(containers[1]),
  surfaceContainer = Color(containers[2]), surfaceContainerHigh = Color(containers[3]),
  surfaceContainerHighest = Color(containers[4]),
  outline = Color(outline), outlineVariant = Color(outlineVariant),
)
