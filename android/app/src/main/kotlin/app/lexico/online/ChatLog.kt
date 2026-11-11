package app.lexico.online

import android.content.Context
import androidx.core.content.edit
import app.lexico.ui.common.ChatRecord
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

class ChatLog(context: Context) {
  private val prefs = context.getSharedPreferences("chat", Context.MODE_PRIVATE)
  private val _records = MutableStateFlow(load())
  val records: StateFlow<List<ChatRecord>> = _records.asStateFlow()

  @Synchronized
  fun add(record: ChatRecord) {
    _records.value = (_records.value + record).takeLast(MAX_RECORDS)
    prefs.edit { putString(KEY, JSONArray(_records.value.map(::encode)).toString()) }
  }

  private fun load(): List<ChatRecord> = runCatching {
    val array = JSONArray(prefs.getString(KEY, null) ?: return emptyList())
    List(array.length()) { decode(array.getJSONObject(it)) }
  }.getOrDefault(emptyList())

  private fun encode(r: ChatRecord): JSONObject = JSONObject()
    .put("at", r.at).put("who", r.who).put("country", r.country).put("text", r.text).put("mine", r.mine).put("room", r.room)

  private fun decode(o: JSONObject): ChatRecord = ChatRecord(
    o.getLong("at"), o.getString("who"), o.optString("country"), o.getString("text"), o.optBoolean("mine"), o.optBoolean("room"),
  )

  private companion object {
    const val KEY = "records"
    const val MAX_RECORDS = 500
  }
}
