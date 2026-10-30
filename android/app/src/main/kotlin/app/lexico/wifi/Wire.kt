package app.lexico.wifi

import org.json.JSONArray
import org.json.JSONObject

internal const val PROTOCOL = 2

internal sealed interface Wire {
  data class Hello(val name: String, val version: Int) : Wire
  data class Welcome(val host: String) : Wire
  data class Refused(val reason: String) : Wire
  data class State(val save: String, val paused: Boolean) : Wire
  data class Move(val input: String) : Wire
  data class Rejected(val reason: String) : Wire
  data object Pause : Wire
  data object Resume : Wire
  data object Bye : Wire
  data object Ping : Wire
  data class Lobby(val players: List<String>) : Wire
  data class Table(val view: String) : Wire
  data object Ready : Wire
  data class Race(val view: String) : Wire
  data class Score(val score: Int, val points: Int, val done: Boolean, val lives: Int) : Wire
  data class RoundDone(val round: Int) : Wire
}

internal fun Wire.encode(): String = when (this) {
  is Wire.Hello -> json("hello").put("name", name).put("version", version)
  is Wire.Welcome -> json("welcome").put("host", host)
  is Wire.Refused -> json("refused").put("reason", reason)
  is Wire.State -> json("state").put("save", save).put("paused", paused)
  is Wire.Move -> json("move").put("input", input)
  is Wire.Rejected -> json("rejected").put("reason", reason)
  Wire.Pause -> json("pause")
  Wire.Resume -> json("resume")
  Wire.Bye -> json("bye")
  Wire.Ping -> json("ping")
  is Wire.Lobby -> json("lobby").put("players", JSONArray(players))
  is Wire.Table -> json("table").put("view", view)
  Wire.Ready -> json("ready")
  is Wire.Race -> json("race").put("view", view)
  is Wire.Score -> json("score").put("score", score).put("points", points).put("done", done).put("lives", lives)
  is Wire.RoundDone -> json("roundDone").put("round", round)
}.toString()

internal fun decode(line: String): Wire? = runCatching { decode(JSONObject(line)) }.getOrNull()

private fun decode(o: JSONObject): Wire? = when (o.getString("t")) {
  "hello" -> Wire.Hello(o.getString("name"), o.getInt("version"))
  "welcome" -> Wire.Welcome(o.getString("host"))
  "refused" -> Wire.Refused(o.getString("reason"))
  "state" -> Wire.State(o.getString("save"), o.getBoolean("paused"))
  "move" -> Wire.Move(o.getString("input"))
  "rejected" -> Wire.Rejected(o.getString("reason"))
  "pause" -> Wire.Pause
  "resume" -> Wire.Resume
  "bye" -> Wire.Bye
  "ping" -> Wire.Ping
  "lobby" -> o.getJSONArray("players").let { a -> Wire.Lobby((0 until a.length()).map(a::getString)) }
  "table" -> Wire.Table(o.getString("view"))
  "ready" -> Wire.Ready
  "race" -> Wire.Race(o.getString("view"))
  "score" -> Wire.Score(o.getInt("score"), o.getInt("points"), o.getBoolean("done"), o.getInt("lives"))
  "roundDone" -> Wire.RoundDone(o.getInt("round"))
  else -> null
}

private fun json(type: String): JSONObject = JSONObject().put("t", type)
