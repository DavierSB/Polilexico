package app.lexico.ui.duplicate

import app.lexico.model.Placement
import app.lexico.model.Board

/*
 * Lo que la pantalla de la duplicada necesita para dibujarse (DuplicateView) y lo que puede
 * pedir (DuplicateActions). El ritmo de cada ronda (ver atril, mano invalida, reloj, ventana
 * para cancelar) lo lleva quien arma la vista; la pantalla solo lo muestra.
 */

/** En que punto de la ronda esta la partida. */
sealed interface Phase {
  /** Entre rondas: falta ver el atril, que arranca el reloj de `turnMs`. */
  data class Waiting(val turnMs: Long) : Phase

  /** El atril no cumple las reglas FISF (vocales/consonantes): se muestra y se vuelve a sacar. */
  data class InvalidRack(val rack: List<String>) : Phase

  /** Pensando tu jugada. `remainingMs` null = sin reloj. */
  data class Playing(val rack: List<String>, val remainingMs: Long?) : Phase

  /** Jugada propuesta; se anota sola cuando vence `cancelMs`, salvo que la canceles. */
  data class Confirming(val rack: List<String>, val placement: String, val remainingMs: Long?, val cancelMs: Long) : Phase

  data object Finished : Phase
}

/** Una jugada de la ronda: "H8 CASA", "pase", "tiempo agotado"... con sus puntos. */
data class RoundPlay(val text: String, val points: Int)

/** Una ronda cerrada: la jugada del master frente a la tuya. */
data class Round(val number: Int, val master: RoundPlay, val mine: RoundPlay, val hit: Boolean)

data class DuplicateView(
  val board: Board,
  val phase: Phase,
  /** Las rondas ya cerradas, en orden. */
  val rounds: List<Round>,
  /** Las fichas que quedan en la bolsa (el atril ya se ve). */
  val bag: List<String>,
  /** Mensaje para el jugador (una jugada rechazada...); null = ninguno. */
  val notice: String? = null,
  /** En pausa: la partida se tapa hasta que el jugador continua. */
  val paused: Boolean = false,
) {
  val myScore: Int get() = rounds.sumOf { it.mine.points }
  val masterScore: Int get() = rounds.sumOf { it.master.points }
  val hits: Int get() = rounds.count { it.hit }

  /** Tus puntos sobre los del master, en %. */
  val efficiency: Double get() = if (masterScore > 0) myScore * 100.0 / masterScore else 0.0

  /** La ronda en juego o, al terminar, la ultima. */
  val round: Int get() = if (phase == Phase.Finished) rounds.size else rounds.size + 1
}

/** Lo que el jugador puede pedir desde la pantalla. */
interface DuplicateActions {
  /** Ver el atril de la ronda (arranca el reloj). */
  fun showRack()
  fun propose(placement: Placement)
  fun pass()
  /** Deshacer la jugada propuesta, dentro de la ventana para cancelar. */
  fun cancel()
  fun resign()
  fun exit()
  /** Revisar la partida terminada. */
  fun analyze()
  fun pause()
  /** Seguir tras la pausa. */
  fun resume()
}
