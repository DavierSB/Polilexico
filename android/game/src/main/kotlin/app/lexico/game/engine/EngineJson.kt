package app.lexico.game.engine

internal data class EngineCandidate(val coords: String, val description: String, val score: Int, val equity: Double)

internal data class EnginePlacement(val placement: String, val score: Int)

private val CANDIDATE = Regex(
  """\{"coords":"([^"]*)","description":"([^"]*)","score":(-?\d+),"equity":(-?[\d.eE+-]+)\}""",
)

private val SCORED_PLACEMENT = Regex("""\{"placement":"([^"]*)","score":(-?\d+)\}""")

internal fun parseCandidates(json: String): List<EngineCandidate> = CANDIDATE.findAll(json).map {
  val (coords, description, score, equity) = it.destructured
  EngineCandidate(coords, description, score.toInt(), equity.toDouble())
}.toList()

internal fun parseScoredPlacements(json: String): List<EnginePlacement> = SCORED_PLACEMENT.findAll(json).map {
  val (placement, score) = it.destructured
  EnginePlacement(placement, score.toInt())
}.toList()
