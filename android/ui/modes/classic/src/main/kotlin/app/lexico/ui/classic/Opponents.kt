package app.lexico.ui.classic

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import app.lexico.ui.board.AppTheme
import app.lexico.ui.board.AppThemes

data class Bot(
  val name: String,
  val alias: String,
  @param:DrawableRes val photo: Int,
  val theme: AppTheme,
  val description: String,
  val voidOnly: Boolean = false,
)

val BOTS = listOf(
  Bot("BeginnerBot", "Polimita", R.drawable.bot_polimita, AppThemes.Leaf, "Polimita va despacio, ideal si justo empiezas.", voidOnly = true),
  Bot("BasicBot", "Sancho", R.drawable.bot_sancho, AppThemes.Polimita, "De tanto cabalgar, Sancho ha aprendido a jugar."),
  Bot("BetterBot", "Pelusa", R.drawable.bot_pelusa, AppThemes.Sky, "¡Cuidado con la mano de Dios!"),
  Bot("STEEBot", "María M.", R.drawable.bot_maria, AppThemes.Polimita, "María escribió un diccionario, ¡y las usará todas contra ti!"),
  Bot("HastyBot", "Gitana", R.drawable.bot_gitana, AppThemes.Night, "La mejor. ¿Qué esconde tras su mirada?"),
)

fun bot(name: String): Bot = BOTS.find { it.name == name || it.alias == name } ?: BOTS.last()

fun alias(name: String): String = BOTS.find { it.name == name }?.alias ?: name

@Composable
fun OpponentPhoto(name: String, size: Dp) {
  Image(painterResource(bot(name).photo), alias(name), Modifier.size(size).clip(CircleShape))
}
