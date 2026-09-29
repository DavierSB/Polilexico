package app.lexico.home

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.lexico.navigation.Screen
import app.lexico.ui.common.Header

/** Un minijuego: su nombre, de que va y a que pantalla lleva. */
private data class Minigame(val name: String, val description: String, val screen: Screen)

private val MINIGAMES = listOf(
  Minigame("Finales", "Practica tus finales, tomando el control de una partida en sus postrimerías.", Screen.NewEndgame),
  Minigame("Scrabble Sprint", "¡Encuentra los scrabbles mientras te queden vidas!", Screen.NewSprint),
  Minigame("¿Cuántas recuerdas?", "Aprende palabras, observando una partida de unos pocos segundos, para luego anagramar sus palabras más valiosas.", Screen.NewRecall),
)

/** Los minijuegos: juegos cortos para entrenar, aparte de las partidas. */
@Composable
fun MinigamesScreen(onBack: () -> Unit, go: (Screen) -> Unit) {
  Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
    Header("Minijuegos", onBack)
    MINIGAMES.forEach { MinigameCard(it) { go(it.screen) } }
  }
}

@Composable
private fun MinigameCard(game: Minigame, open: () -> Unit) {
  Column(
    Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
      .clickable(onClick = open).padding(12.dp),
  ) {
    Text(game.name, fontWeight = FontWeight.Bold)
    Text(game.description, style = MaterialTheme.typography.bodySmall)
  }
}
