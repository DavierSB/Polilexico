package app.lexico.games

import app.lexico.game.records.FinishedGame
import app.lexico.game.records.RecordSeat
import app.lexico.game.storage.Mode
import app.lexico.ui.games.Face

private const val YOU = "Tú"

internal enum class RecordKind { BOT, LOCAL, ONLINE, WATCHED }

internal val FinishedGame.kind: RecordKind
  get() = when {
    watched -> RecordKind.WATCHED
    online -> RecordKind.ONLINE
    host != null -> RecordKind.LOCAL
    else -> RecordKind.BOT
  }

internal val FinishedGame.bottomName: String
  get() = if (mode != Mode.DUPLICATE && kind in NAMED && me.name.isNotEmpty()) me.name else YOU

internal val FinishedGame.needsPhotos: Boolean
  get() = mode != Mode.DUPLICATE && kind in PEOPLE

internal fun FinishedGame.myFace(): Face? = if (needsPhotos && kind == RecordKind.WATCHED) person(me) else null

internal fun FinishedGame.opponentFace(): Face? = when {
  mode == Mode.DUPLICATE || kind == RecordKind.LOCAL -> null
  needsPhotos -> person(rival)
  else -> Face.Bot
}

private fun person(seat: RecordSeat): Face = Face.Person(seat.country, seat.id)

private val NAMED = setOf(RecordKind.LOCAL, RecordKind.WATCHED)

private val PEOPLE = setOf(RecordKind.ONLINE, RecordKind.WATCHED)
