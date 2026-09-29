package app.lexico.ui.board

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import app.lexico.model.Bonus

/**
 * Todo lo visual del tablero. Para un estilo propio: `BoardStyles.Isc.copy(tile = ...)`.
 * Las medidas relativas (`rounding`, `lineWidth`) son fracciones del lado de una casilla, asi
 * que el estilo se ve igual a cualquier tamano.
 */
@Immutable
data class BoardStyle(
  val name: String,
  val lines: Color,
  val lineWidth: Float = 0.04f,
  val square: Color,
  val doubleLetter: Color,
  val tripleLetter: Color,
  val doubleWord: Color,
  val tripleWord: Color,
  val center: Color = doubleWord,
  val bonusText: Color,
  /** Textos de cada premio; vacio = sin texto. */
  val labels: Map<Bonus, String> = SHORT_LABELS,
  val tile: Color,
  val tileBorder: Color = Color.Transparent,
  val letter: Color,
  val blank: Color = letter,
  val value: Color = letter,
  val showValues: Boolean = true,
  /** Fondo de las fichas de la ultima jugada; `null` = no resaltar. */
  val latest: Color? = null,
  /** Fondo de las fichas que el usuario esta colocando. */
  val pending: Color,
  /** Casilla de la flecha de escritura y la flecha en si. */
  val arrow: Color = Color(0xFFFFB631),
  val arrowInk: Color = Color(0xFF2A140C),
  /** Los puntos de cada jugada ("+34"), sin fondo: del color de la app y, las jugadas grandes, en rojo. */
  val scoreInk: Color = Color(0xFFFFB631),
  val highScoreInk: Color = Color(0xFFFF5252),
  val rounding: Float = 0.12f,
  /** Letras A-O y numeros 1-15 alrededor del tablero. */
  val showCoordinates: Boolean = true,
  val coordinates: Color = bonusText,
  val backdrop: Color = lines,
  val fontFamily: FontFamily = FontFamily.SansSerif,
  val fontWeight: FontWeight = FontWeight.Bold,
) {
  fun bonusColor(p: Bonus): Color = when (p) {
    Bonus.NONE -> square
    Bonus.DOUBLE_LETTER -> doubleLetter
    Bonus.TRIPLE_LETTER -> tripleLetter
    Bonus.DOUBLE_WORD -> doubleWord
    Bonus.TRIPLE_WORD -> tripleWord
    Bonus.CENTER -> center
  }

  companion object {
    val SHORT_LABELS = mapOf(
      Bonus.DOUBLE_LETTER to "DL", Bonus.TRIPLE_LETTER to "TL",
      Bonus.DOUBLE_WORD to "DP", Bonus.TRIPLE_WORD to "TP", Bonus.CENTER to "★",
    )
    val STAR_ONLY = mapOf(Bonus.CENTER to "★")
    val NUMBER_LABELS = mapOf(
      Bonus.DOUBLE_LETTER to "2L", Bonus.TRIPLE_LETTER to "3L",
      Bonus.DOUBLE_WORD to "2P", Bonus.TRIPLE_WORD to "3P", Bonus.CENTER to "★",
    )
  }
}

/** Estilos listos para usar. [ALL] sirve para un selector. */
object BoardStyles {
  /**
   * El de la app: oscuro y calido, en los colores de la polimita. Las casillas de letra en ocre
   * y oro, las de palabra en rojo oxido y terracota; las fichas en crema.
   */
  val Polimita = BoardStyle(
    name = "Polimita",
    lines = Color(0xFF1B1411),
    square = Color(0xFF2A201B),
    doubleLetter = Color(0xFF80601C),
    tripleLetter = Color(0xFFD0982A),
    doubleWord = Color(0xFF8C3822),
    tripleWord = Color(0xFFD9522A),
    bonusText = Color(0xFFFFF1DE),
    labels = BoardStyle.NUMBER_LABELS,
    tile = Color(0xFFF5E6CC),
    letter = Color(0xFF2A140C),
    blank = Color(0xFFD9522A),
    latest = Color(0xFFFFC85A),
    pending = Color(0xFFF7B27A),
    highScoreInk = Color(0xFFFF6B4A),
    rounding = 0.18f,
    coordinates = Color(0xFF9E8A7A),
  )

  /** Hoja y flor: letras en verdes de hoja, palabras en fucsia de flor tropical. */
  val Leaf = BoardStyle(
    name = "Hoja",
    lines = Color(0xFF0E140D),
    square = Color(0xFF1A2419),
    doubleLetter = Color(0xFF2E6A36),
    tripleLetter = Color(0xFF3FAA55),
    doubleWord = Color(0xFF86294F),
    tripleWord = Color(0xFFD9457F),
    bonusText = Color(0xFFF2F6EE),
    labels = BoardStyle.NUMBER_LABELS,
    tile = Color(0xFFF6F3E6),
    letter = Color(0xFF10200C),
    blank = Color(0xFFD9457F),
    latest = Color(0xFFF2D36B),
    pending = Color(0xFFF3B6CC),
    arrow = Color(0xFF8FD46A),
    arrowInk = Color(0xFF10200C),
    scoreInk = Color(0xFF8FD46A),
    highScoreInk = Color(0xFFFF6B9A),
    rounding = 0.18f,
    coordinates = Color(0xFF8A9A80),
  )

  /** Celeste y oro: letras en celestes, palabras en el oro del sol de mayo. */
  val Sky = BoardStyle(
    name = "Celeste",
    lines = Color(0xFF0F1620),
    square = Color(0xFF1C2733),
    doubleLetter = Color(0xFF34597A),
    tripleLetter = Color(0xFF5A94C8),
    doubleWord = Color(0xFF6B5418),
    tripleWord = Color(0xFFB88A12),
    bonusText = Color(0xFFE8F0F8),
    labels = BoardStyle.NUMBER_LABELS,
    tile = Color(0xFFF4F7FA),
    letter = Color(0xFF0B1E33),
    blank = Color(0xFF34597A),
    latest = Color(0xFFF6D26B),
    pending = Color(0xFFA9CFEF),
    arrow = Color(0xFF74ACDF),
    arrowInk = Color(0xFF0B1E33),
    scoreInk = Color(0xFF74ACDF),
    highScoreInk = Color(0xFFFF6B5A),
    rounding = 0.18f,
    coordinates = Color(0xFF8595A6),
  )

  /** Plano y sencillo, a la manera de isc.ro: casillas lisas, fichas amarillas, sin adornos. */
  val Isc = BoardStyle(
    name = "ISC",
    lines = Color(0xFF9E9E8E),
    square = Color(0xFFD9D9C4),
    doubleLetter = Color(0xFF9BC4E2),
    tripleLetter = Color(0xFF3C78B4),
    doubleWord = Color(0xFFF2B6B6),
    tripleWord = Color(0xFFD23C3C),
    bonusText = Color(0xFF3A3A30),
    labels = emptyMap(),
    tile = Color(0xFFFFEE88),
    tileBorder = Color(0xFF8C7A2A),
    letter = Color(0xFF111111),
    blank = Color(0xFFB02020),
    latest = Color(0xFFBFF0A0),
    pending = Color(0xFFFFFFFF),
    rounding = 0f,
  )

  /** El tablero de carton de toda la vida. */
  val Classic = BoardStyle(
    name = "Clásico",
    lines = Color(0xFFF4F1E4),
    lineWidth = 0.06f,
    square = Color(0xFFC9C3A5),
    doubleLetter = Color(0xFFB9DCEB),
    tripleLetter = Color(0xFF2E86C1),
    doubleWord = Color(0xFFF5B7B1),
    tripleWord = Color(0xFFC0392B),
    bonusText = Color(0xFF2B2B2B),
    tile = Color(0xFFF3DDAE),
    tileBorder = Color(0xFFB08D57),
    letter = Color(0xFF2B2014),
    blank = Color(0xFF9A6B2F),
    latest = Color(0xFFFFD27A),
    pending = Color(0xFFFFF7E6),
    fontFamily = FontFamily.Serif,
  )

  /** Oscuro y de contraste suave, parecido a Woogles. */
  val Night = BoardStyle(
    name = "Noche",
    lines = Color(0xFF1B1E24),
    square = Color(0xFF2B2F37),
    doubleLetter = Color(0xFF2F5D7C),
    tripleLetter = Color(0xFF3E7FC1),
    doubleWord = Color(0xFF7C3B4A),
    tripleWord = Color(0xFFB8434F),
    bonusText = Color(0xFFB9C0CC),
    labels = BoardStyle.NUMBER_LABELS,
    tile = Color(0xFFE9E4D4),
    letter = Color(0xFF1B1E24),
    blank = Color(0xFF3E7FC1),
    latest = Color(0xFFFFD166),
    pending = Color(0xFF9FE3B0),
    arrow = Color(0xFF7FA6E8),
    arrowInk = Color(0xFF0A1A33),
    scoreInk = Color(0xFF7FA6E8),
    rounding = 0.18f,
    coordinates = Color(0xFF8A93A3),
  )

  /** Blanco y negro, lo minimo: bueno para capturas o para imprimir. */
  val Paper = BoardStyle(
    name = "Papel",
    lines = Color(0xFFBDBDBD),
    square = Color(0xFFFFFFFF),
    doubleLetter = Color(0xFFE8E8E8),
    tripleLetter = Color(0xFFBDBDBD),
    doubleWord = Color(0xFFD6D6D6),
    tripleWord = Color(0xFF9E9E9E),
    bonusText = Color(0xFF616161),
    tile = Color(0xFF212121),
    letter = Color(0xFFFFFFFF),
    blank = Color(0xFFBDBDBD),
    pending = Color(0xFF757575),
    scoreInk = Color(0xFFB85A0F),
    highScoreInk = Color(0xFFD32F2F),
    rounding = 0.08f,
  )

  val ALL = listOf(Polimita, Leaf, Sky, Night, Isc, Classic, Paper)

  /** El estilo con ese nombre, o [Polimita] si no hay ninguno. */
  fun byName(name: String?): BoardStyle = ALL.find { it.name == name } ?: Polimita

  /** El siguiente de [ALL], para un boton que los va rotando. */
  fun next(style: BoardStyle): BoardStyle = ALL[(ALL.indexOf(style) + 1) % ALL.size]
}
