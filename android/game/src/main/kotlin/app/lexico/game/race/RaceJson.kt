package app.lexico.game.race

import app.lexico.game.modes.recall.ScoredPlacement
import org.json.JSONArray
import org.json.JSONObject

fun RaceView.toJson(): String = JSONObject()
  .put("setup", setup.toJson()).put("over", over).put("remainingMs", remainingMs ?: -1)
  .put("runners", JSONArray(runners.map { it.toJson() })).put("round", round?.toJson()).toString()

fun readRace(json: String): RaceView = JSONObject(json).let { o ->
  RaceView(
    readSetup(o.getJSONObject("setup")), o.getBoolean("over"), o.getLong("remainingMs").takeIf { it >= 0 },
    o.getJSONArray("runners").let { a -> (0 until a.length()).map { readRunner(a.getJSONObject(it)) } },
    o.optJSONObject("round")?.let(::readRound),
  )
}

private fun RaceSetup.toJson(): JSONObject = JSONObject()
  .put("game", game.key).put("timeMs", timeMs).put("goal", goal).put("rounds", rounds)
  .put("difficulty", difficulty).put("intervalMs", intervalMs).put("single", single).put("roundMs", roundMs)

private fun readSetup(o: JSONObject): RaceSetup = RaceSetup(
  RaceGame.entries.first { it.key == o.getString("game") }, o.getLong("timeMs"), o.getInt("goal"), o.getInt("rounds"),
  o.getString("difficulty"), o.getLong("intervalMs"), o.getBoolean("single"), o.getLong("roundMs"),
)

private fun Runner.toJson(): JSONObject = JSONObject()
  .put("name", name).put("score", score).put("points", points).put("done", done).put("present", present)
  .put("lives", lives).put("ready", ready)

private fun readRunner(o: JSONObject): Runner = Runner(
  o.getString("name"), o.getInt("score"), o.getInt("points"), o.getBoolean("done"), o.getBoolean("present"),
  o.getInt("lives"), o.getBoolean("ready"),
)

private fun RaceRound.toJson(): JSONObject = JSONObject()
  .put("number", number).put("open", open).put("remainingMs", remainingMs ?: -1)
  .put("game", JSONArray(game.map { JSONObject().put("placement", it.placement).put("score", it.score) }))

private fun readRound(o: JSONObject): RaceRound = RaceRound(
  o.getInt("number"), o.getBoolean("open"),
  o.getJSONArray("game").let { a -> (0 until a.length()).map { a.getJSONObject(it).let { p -> ScoredPlacement(p.getString("placement"), p.getInt("score")) } } },
  o.getLong("remainingMs").takeIf { it >= 0 },
)
