package app.lexico.online

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject

internal const val OFFLINE = "No se pudo conectar con el servidor."

class OnlineError(message: String) : Exception(message)

sealed interface PhotoReply {
  class Fresh(val bytes: ByteArray, val etag: String?) : PhotoReply
  data object Same : PhotoReply
  data object None : PhotoReply
  data object Unreachable : PhotoReply
}

data class AccountForm(val name: String, val country: String, val password: String, val email: String = "")

data class OnlineStatsRow(
  val mode: String, val games: Int, val wins: Int, val losses: Int, val draws: Int,
  val bestBingo: Int, val bestPlay: Int, val avgScore: Double, val avgEfficiency: Double?,
)

class OnlineApi(private val server: () -> String, private val http: OkHttpClient) {
  suspend fun signUp(form: AccountForm): Result<OnlineSession> = call("POST", "/signup", form.json()).map(::session)

  suspend fun logIn(form: AccountForm): Result<OnlineSession> = call("POST", "/login", form.json()).map(::session)

  suspend fun refresh(refreshToken: String): Result<OnlineSession> =
    call("POST", "/refresh", JSONObject().put("refreshToken", refreshToken)).map(::session)

  suspend fun stats(token: String): Result<List<OnlineStatsRow>> = call("GET", "/stats", token = token).map { statsRows(it.optJSONArray("rows")) }

  suspend fun account(token: String): Result<AccountForm> = call("GET", "/account", token = token).map(::accountForm)

  suspend fun updateAccount(token: String, form: AccountForm): Result<AccountForm> =
    call("PUT", "/account", form.json(), token).map(::accountForm)

  private fun accountForm(json: JSONObject): AccountForm =
    AccountForm(json.getString("name"), json.getString("country"), "", json.optString("email"))

  suspend fun savePhoto(token: String, base64: String): Result<Unit> =
    call("PUT", "/photo", JSONObject().put("data", base64), token).map { }

  suspend fun photo(name: String, country: String, etag: String?): PhotoReply = withContext(Dispatchers.IO) {
    runCatching { http.newCall(photoRequest(name, country, etag)).execute() }.fold({ r -> r.use(::photoReply) }) { PhotoReply.Unreachable }
  }

  private fun photoRequest(name: String, country: String, etag: String?): Request =
    Request.Builder().url("${server()}/photo".toHttpUrl().newBuilder().addQueryParameter("name", name).addQueryParameter("country", country).build())
      .apply { etag?.let { header("If-None-Match", it) } }.build()

  private fun photoReply(r: Response): PhotoReply = when (r.code) {
    200 -> PhotoReply.Fresh(r.body?.bytes() ?: ByteArray(0), r.header("ETag"))
    304 -> PhotoReply.Same
    404 -> PhotoReply.None
    else -> PhotoReply.Unreachable
  }

  suspend fun presence(): Result<Int> = call("GET", "/presence").map { it.optInt("online") }

  suspend fun backup(token: String): Result<String> = call("GET", "/backup", token = token).map { it.optString("data") }

  suspend fun saveBackup(token: String, data: String): Result<Unit> =
    call("PUT", "/backup", JSONObject().put("data", data), token).map { }

  private suspend fun call(method: String, path: String, body: JSONObject? = null, token: String? = null): Result<JSONObject> =
    withContext(Dispatchers.IO) {
      runCatching { http.newCall(request(method, path, body, token)).execute() }
        .fold({ response -> response.use { parsed(it.code, it.body?.string().orEmpty()) } }) { Result.failure(OnlineError(OFFLINE)) }
    }

  private fun request(method: String, path: String, body: JSONObject?, token: String?): Request =
    Request.Builder().url(server() + path)
      .method(method, body?.toString()?.toRequestBody(JSON))
      .apply { token?.let { header("Authorization", "Bearer $it") } }
      .build()

  private fun parsed(code: Int, text: String): Result<JSONObject> {
    val json = runCatching { JSONObject(text) }.getOrNull() ?: return Result.failure(OnlineError(OFFLINE))
    return if (code in 200..299) Result.success(json) else Result.failure(OnlineError(json.optString("error", OFFLINE)))
  }

  private fun session(json: JSONObject): OnlineSession = OnlineSession(
    json.getString("accessToken"), json.getString("refreshToken"),
    System.currentTimeMillis() + json.optLong("expiresIn", 3600) * 1000, json.getString("name"), json.getString("country"),
  )

  private fun statsRows(rows: JSONArray?): List<OnlineStatsRow> = (0 until (rows?.length() ?: 0)).map { i ->
    val r = rows!!.getJSONObject(i)
    OnlineStatsRow(
      r.getString("mode"), r.optInt("games"), r.optInt("wins"), r.optInt("losses"), r.optInt("draws"),
      r.optInt("best_bingo"), r.optInt("best_play"), r.optDouble("avg_score", 0.0),
      r.optDouble("avg_efficiency").takeUnless { it.isNaN() },
    )
  }

  private fun AccountForm.json(): JSONObject =
    JSONObject().put("name", name).put("country", country).put("password", password).put("email", email)

  private companion object {
    val JSON = "application/json".toMediaType()
  }
}
