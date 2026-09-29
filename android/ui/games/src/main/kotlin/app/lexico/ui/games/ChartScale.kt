package app.lexico.ui.games

import kotlin.math.ceil
import kotlin.math.floor

/** Los pasos posibles entre marcas del eje, de menor a mayor. */
private val STEPS = listOf(10, 20, 25, 50, 100, 200, 250, 500)

/** Como mucho, estas marcas en un eje. */
private const val MAX_TICKS = 5

/**
 * La escala de un eje: de `min` a `max`, con aire alrededor de los valores y redondeada a
 * decenas, y unas marcas redondas (cada 50, 100, 200...). `cap` limita el maximo (100 en %).
 */
internal class ChartScale(values: List<Int>, cap: Int? = null) {
  val min: Int
  val max: Int

  init {
    val lo = values.minOrNull() ?: 0
    val hi = values.maxOrNull() ?: 100
    val air = maxOf(20, (hi - lo) / 10)
    min = (floor((lo - air) / 10.0) * 10).toInt().coerceAtLeast(0)
    max = (ceil((hi + air) / 10.0) * 10).toInt().coerceAtLeast(min + 10).let { if (cap != null) minOf(it, cap) else it }
  }

  /** Las marcas del eje, dentro de la escala. */
  val ticks: List<Int>
    get() {
      val step = STEPS.firstOrNull { (max - min) / it <= MAX_TICKS } ?: 1000
      return ((ceil(min / step.toDouble()) * step).toInt()..max step step).toList()
    }

  /** Donde cae `value`, de 0 (el minimo) a 1 (el maximo). */
  fun fraction(value: Int): Float = (value - min).toFloat() / (max - min)
}
