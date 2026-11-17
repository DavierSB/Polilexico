package app.lexico.games

import app.lexico.game.Outcome
import app.lexico.game.records.FinishedGame
import app.lexico.game.records.RecordSeat
import app.lexico.game.storage.Mode
import app.lexico.ui.games.Face
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class ReviewPlayersTest {
  private val ana = RecordSeat("Ana", "CU", "a-1")
  private val beto = RecordSeat("Beto", "ES", "b-2")

  private fun game(opponent: String = "Beto", host: String? = null, watched: Boolean = false, online: Boolean = false, mode: Mode = Mode.CLASSIC) =
    FinishedGame("p", mode, "", opponent, 1, 2, Outcome.LOSS, host = host, me = ana, rival = beto, watched = watched, online = online)

  @Test fun watchedGameNamesAndShowsBothPlayers() {
    val g = game(host = "Ana", watched = true)
    assertEquals("Ana", g.bottomName)
    assertEquals(Face.Person("CU", "a-1"), g.myFace())
    assertEquals(Face.Person("ES", "b-2"), g.opponentFace())
  }

  @Test fun onlineGameKeepsTuAndShowsTheRival() {
    val g = game(host = "Ana", online = true)
    assertEquals("Tú", g.bottomName)
    assertNull(g.myFace())
    assertEquals(Face.Person("ES", "b-2"), g.opponentFace())
  }

  @Test fun botGameIsUnchanged() {
    val g = game(opponent = "HastyBot").copy(me = RecordSeat("Tú"), rival = RecordSeat("HastyBot"))
    assertEquals("Tú", g.bottomName)
    assertEquals(Face.Bot, g.opponentFace())
    assertFalse(g.needsPhotos)
  }

  @Test fun localGameNamesWithoutPhotos() {
    val g = game(host = "Ana").copy(me = RecordSeat("Ana"), rival = RecordSeat("Beto"))
    assertEquals("Ana", g.bottomName)
    assertNull(g.myFace())
    assertNull(g.opponentFace())
  }

  @Test fun oldOnlineRecordFallsBackToTheInitial() {
    val g = game(host = "Ana", online = true).copy(rival = RecordSeat("Beto"))
    assertEquals(Face.Person("", ""), g.opponentFace())
  }

  @Test fun duplicateHasNoFaces() {
    val g = game(opponent = "Máster", watched = true, mode = Mode.DUPLICATE)
    assertEquals("Tú", g.bottomName)
    assertNull(g.opponentFace())
  }
}
