package app.lexico.ui.games

data class StatsFilter(val mode: StatsMode, val opponent: String? = null)

enum class StatsMode { CLASSIC, DUPLICATE }

data class StatsPage(val rows: List<Pair<String, String>>, val emptyText: String, val note: String?, val games: List<ChartGame>)

data class ChartGame(val myScore: Int, val opponentScore: Int, val value: Int, val title: String, val lines: List<String>)
