package app.lexico.online

import org.json.JSONArray
import org.json.JSONObject

internal const val ONLINE_PROTOCOL = 4

data class RoomInfo(
  val id: String, val duplicate: Boolean, val me: String, val host: String, val players: List<String>, val first: Boolean,
  val watching: Boolean = false, val timed: Boolean = true,
)

data class LiveGame(
  val id: String, val duplicate: Boolean, val players: List<String>, val countries: List<String>, val watchers: Int = 0,
  val scores: List<Int> = emptyList(), val bag: Int = 0, val round: Int = 0, val leader: String = "", val efficiency: Int = 0,
  val tied: Int = 0,
)

data class Seek(
  val id: String, val duplicate: Boolean, val host: String, val country: String, val timeMs: Long, val overtimeMs: Long,
  val turnMs: Long, val rounds: Int, val players: Int, val mine: Boolean, val single: Boolean = true, val timed: Boolean = true,
  val to: String = "", val resume: Boolean = false,
)

data class Person(val name: String, val country: String, val busy: Boolean)

data class AdjournedGame(
  val id: String, val opponent: String, val country: String, val online: Boolean, val busy: Boolean = false, val result: String = "",
  val duplicate: Boolean = false, val untimed: Boolean = false,
)

data class Lobby(
  val seeks: List<Seek> = emptyList(), val invites: List<Seek> = emptyList(), val people: List<Person> = emptyList(),
  val games: List<LiveGame> = emptyList(), val adjourned: List<AdjournedGame> = emptyList(),
)

sealed interface Proposal {
  data class Classic(val timeMs: Long, val overtimeMs: Long, val single: Boolean = true, val timed: Boolean = true) : Proposal
  data class Duplicate(val turnMs: Long, val rounds: Int, val live: Boolean) : Proposal
}

internal sealed interface OnlineEvent {
  data object Welcome : OnlineEvent
  data class Refused(val reason: String) : OnlineEvent
  data class Failed(val reason: String) : OnlineEvent
  data class LobbyChanged(val lobby: Lobby) : OnlineEvent
  data class Room(val info: RoomInfo) : OnlineEvent
  data class State(val save: String, val first: Boolean) : OnlineEvent
  data class Table(val view: String) : OnlineEvent
  data class Rejected(val reason: String) : OnlineEvent
  data class Chat(val from: String, val text: String) : OnlineEvent
  data class Watchers(val people: List<Person>) : OnlineEvent
  data class Rival(val online: Boolean, val awayMs: Long) : OnlineEvent
  data class AdjournAsk(val name: String) : OnlineEvent
  data class Tell(val name: String, val country: String, val text: String) : OnlineEvent
  data class Closed(val reason: String) : OnlineEvent
  data class DuplicateOffer(val code: String) : OnlineEvent
}

internal fun hello(token: String): String = message("hello").put("version", ONLINE_PROTOCOL).put("token", token).toString()

internal fun create(p: Proposal): String = proposal(message("create"), p).toString()

internal fun challengeMessage(name: String, country: String, p: Proposal.Classic): String =
  proposal(message("challenge").put("name", name).put("country", country), p).toString()

internal fun roomAction(type: String, id: String): String = message(type).put("code", id).toString()

internal fun moveMessage(input: String): String = message("move").put("input", input).toString()

internal fun tellMessage(name: String, country: String, text: String): String =
  message("tell").put("name", name).put("country", country).put("text", text).toString()

internal fun roomChatMessage(text: String): String = message("chat").put("text", text).toString()

internal fun signal(type: String): String = message(type).toString()

internal fun ping(): String = signal("ping")

internal fun adjournReply(accept: Boolean): String = message("adjournReply").put("accept", accept).toString()

internal fun event(text: String): OnlineEvent? = runCatching { event(JSONObject(text)) }.getOrNull()

private fun proposal(o: JSONObject, p: Proposal): JSONObject = when (p) {
  is Proposal.Classic -> o.put("mode", "classic").put("timeMs", p.timeMs).put("overtimeMs", p.overtimeMs)
    .put("void", !p.single).put("untimed", !p.timed)
  is Proposal.Duplicate -> o.put("mode", "duplicate").put("turnMs", p.turnMs).put("rounds", p.rounds).put("live", p.live)
}

private fun event(o: JSONObject): OnlineEvent? = when (o.getString("t")) {
  "welcome" -> OnlineEvent.Welcome
  "refused" -> OnlineEvent.Refused(o.optString("reason"))
  "error" -> OnlineEvent.Failed(o.optString("reason"))
  "lobby" -> OnlineEvent.LobbyChanged(
    Lobby(
      seeks(o.optJSONArray("seeks")), seeks(o.optJSONArray("invites")), people(o.optJSONArray("people")), games(o.optJSONArray("games")),
      adjourned(o.optJSONArray("adjourned")),
    ),
  )
  "watchers" -> OnlineEvent.Watchers(people(o.optJSONArray("people")))
  "room" -> OnlineEvent.Room(roomInfo(o))
  "state" -> OnlineEvent.State(o.getString("save"), o.optBoolean("first"))
  "table" -> OnlineEvent.Table(o.getString("view"))
  "rejected" -> OnlineEvent.Rejected(o.optString("reason"))
  "chat" -> OnlineEvent.Chat(o.optString("name"), o.optString("text"))
  "tell" -> OnlineEvent.Tell(o.optString("name"), o.optString("country"), o.optString("text"))
  "duplicateOffer" -> OnlineEvent.DuplicateOffer(o.optString("code"))
  "rival" -> OnlineEvent.Rival(o.optBoolean("online"), o.optLong("awayMs"))
  "adjournAsk" -> OnlineEvent.AdjournAsk(o.optString("name"))
  "closed" -> OnlineEvent.Closed(o.optString("reason"))
  else -> null
}

private fun roomInfo(o: JSONObject): RoomInfo = RoomInfo(
  o.getString("code"), o.optString("mode") == "duplicate", o.optString("name"), o.optString("host"), strings(o.optJSONArray("players")),
  o.optBoolean("first"), o.optBoolean("watching"), !o.optBoolean("untimed"),
)

private fun games(a: JSONArray?): List<LiveGame> = objects(a).map {
  LiveGame(
    it.getString("id"), it.optString("mode") == "duplicate", strings(it.optJSONArray("players")), strings(it.optJSONArray("countries")),
    it.optInt("watchers"), ints(it.optJSONArray("scores")), it.optInt("bag"), it.optInt("round"), it.optString("leader"), it.optInt("efficiency"), it.optInt("tied"),
  )
}

private fun seeks(a: JSONArray?): List<Seek> = objects(a).map { s ->
  Seek(
    s.getString("id"), s.optString("mode") == "duplicate", s.optString("host"), s.optString("country"), s.optLong("timeMs"),
    s.optLong("overtimeMs"), s.optLong("turnMs"), s.optInt("rounds"), s.optInt("players"), s.optBoolean("mine"),
    !s.optBoolean("void"), !s.optBoolean("untimed"), s.optString("to"), s.optBoolean("resume"),
  )
}

private fun adjourned(a: JSONArray?): List<AdjournedGame> = objects(a).map {
  AdjournedGame(
    it.getString("id"), it.optString("opponent"), it.optString("country"), it.optBoolean("online"), it.optBoolean("busy"),
    it.optString("result"), it.optBoolean("duplicate"), it.optBoolean("untimed"),
  )
}

private fun people(a: JSONArray?): List<Person> = objects(a).map { Person(it.optString("name"), it.optString("country"), it.optBoolean("playing")) }

private fun objects(a: JSONArray?): List<JSONObject> = (0 until (a?.length() ?: 0)).map { a!!.getJSONObject(it) }

private fun strings(a: JSONArray?): List<String> = (0 until (a?.length() ?: 0)).map { a!!.getString(it) }

private fun ints(a: JSONArray?): List<Int> = (0 until (a?.length() ?: 0)).map { a!!.getInt(it) }

private fun message(type: String): JSONObject = JSONObject().put("t", type)
