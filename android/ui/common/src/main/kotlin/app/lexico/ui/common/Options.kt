package app.lexico.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/*
 * Controles de las pantallas de "partida nueva", comunes a las modalidades.
 */

/** Dos opciones excluyentes; `secondSelected` dice cual esta marcada. */
@Composable
fun TwoOptions(first: String, second: String, secondSelected: Boolean, onChange: (secondSelected: Boolean) -> Unit) {
  Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    Option(first, selected = !secondSelected) { onChange(false) }
    Option(second, selected = secondSelected) { onChange(true) }
  }
}

/** Un tiempo escrito como "20" o "3:20"; se marca en rojo si no se entiende. */
@Composable
fun TimeField(value: String, onChange: (String) -> Unit, label: String) {
  OutlinedTextField(
    value = value, onValueChange = onChange, singleLine = true,
    label = { Text("$label (min o min:seg)") },
    isError = Durations.parse(value) == null,
    modifier = Modifier.fillMaxWidth(),
  )
}

/**
 * Void (una palabra que no esta en FILE2017 se rechaza y se puede corregir) o single (la jugada
 * no entra y se pierde el turno). `penalty` completa la explicacion de single en cada modalidad.
 */
@Composable
fun ChallengeModeSelector(single: Boolean, onChange: (Boolean) -> Unit, penalty: String) {
  SectionTitle("Comprobación de jugadas")
  TwoOptions("Void", "Single", single, onChange)
  Hint(if (single) "Single: si pones palabras no válidas, la jugada no entra y $penalty." else VOID_HINT)
}

/** El titulo de una seccion de opciones ("Rival", "Tiempo"...). */
@Composable
fun SectionTitle(text: String) {
  Text(text, style = MaterialTheme.typography.labelLarge)
}

/** Una explicacion breve bajo una opcion. */
@Composable
fun Hint(text: String) {
  Text(text, style = MaterialTheme.typography.bodySmall)
}

/** Un numero entero entre los limites de `range`, con − y +. */
@Composable
fun Stepper(value: Int, range: IntRange, onChange: (Int) -> Unit) {
  Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
    OutlinedButton(onClick = { onChange(value - 1) }, enabled = value > range.first) { Text("−") }
    Text("$value", Modifier.width(40.dp), fontSize = 22.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
    OutlinedButton(onClick = { onChange(value + 1) }, enabled = value < range.last) { Text("+") }
  }
}

/** El boton que empieza la partida con las opciones elegidas. */
@Composable
fun StartButton(enabled: Boolean, onClick: () -> Unit) {
  Button(enabled = enabled, modifier = Modifier.fillMaxWidth(), onClick = onClick) { Text("Empezar") }
}

@Composable
private fun Option(text: String, selected: Boolean, select: () -> Unit) {
  if (selected) Button(onClick = {}) { Text(text) } else OutlinedButton(onClick = select) { Text(text) }
}

private const val VOID_HINT = "Void: una jugada con palabras no válidas se rechaza y puedes volver a intentarlo."
