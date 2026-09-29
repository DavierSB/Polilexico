package app.lexico.ui.games

/** Que estadisticas se ven: la modalidad y, en clasica, contra que rival (null = todos). */
data class StatsFilter(val mode: StatsMode, val opponent: String? = null)

enum class StatsMode { CLASSIC, DUPLICATE }

/**
 * Las estadisticas para un filtro: la tabla de datos (vacia = no hay partidas, y entonces se
 * muestra `emptyText`), una nota bajo la tabla y las partidas para las graficas.
 */
data class StatsPage(val rows: List<Pair<String, String>>, val emptyText: String, val note: String?, val games: List<ChartGame>)

/**
 * Una partida en las graficas: sus puntos y los del rival (para la dispersion), el valor de la
 * tira (tus puntos, o la eficiencia en duplicada) y lo que se ve al tocarla.
 */
data class ChartGame(val myScore: Int, val opponentScore: Int, val value: Int, val title: String, val lines: List<String>)
