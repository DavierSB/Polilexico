package app.lexico.ui.common

import androidx.compose.runtime.Composable

@Composable
fun StudyModeSelector(on: Boolean, onChange: (Boolean) -> Unit) {
  SectionTitle("Modo Estudio", info = STUDY_INFO)
  TwoOptions("Off", "On", on, onChange)
}

private const val STUDY_INFO =
  "Con el Modo Estudio activado, durante la partida se puede tocar una palabra ya jugada en el tablero para ver su " +
    "significado en el diccionario."
