package app.lexico.game.engine

/*
 * Las listas en JSON que devuelve el motor. Son planas y sin comillas dentro de los textos, asi
 * que bastan unas expresiones regulares (y se prueban en la PC, sin Android).
 */

/** Una jugada candidata del motor: `description` es "H8 CASA (12 pts)", "(Pasar)"... */
internal data class EngineCandidate(val coords: String, val description: String, val score: Int, val equity: Double)

/** Una colocacion de una partida de demostracion, con sus puntos. */
internal data class EnginePlacement(val placement: String, val score: Int)

private val CANDIDATE = Regex(
  """\{"coords":"([^"]*)","description":"([^"]*)","score":(-?\d+),"equity":(-?[\d.eE+-]+)\}""",
)

private val SCORED_PLACEMENT = Regex("""\{"placement":"([^"]*)","score":(-?\d+)\}""")

/** `[{"coords","description","score","equity"}, ...]` de engine.BestMoves. */
internal fun parseCandidates(json: String): List<EngineCandidate> = CANDIDATE.findAll(json).map {
  val (coords, description, score, equity) = it.destructured
  EngineCandidate(coords, description, score.toInt(), equity.toDouble())
}.toList()

/** `[{"placement","score"}, ...]` de demo.PlayWithScores. */
internal fun parseScoredPlacements(json: String): List<EnginePlacement> = SCORED_PLACEMENT.findAll(json).map {
  val (placement, score) = it.destructured
  EnginePlacement(placement, score.toInt())
}.toList()
