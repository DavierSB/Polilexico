package app.lexico.ui.classic

import app.lexico.model.Placement
import app.lexico.model.Board

/*
 * Lo que la pantalla de la clasica necesita para dibujarse (ClassicView) y lo que puede pedir
 * (ClassicActions). Quien la use -- el juego, o datos de ejemplo -- arma la vista; la pantalla
 * no sabe de donde sale.
 */

enum class Side { ME, OPPONENT }

/** Las fichas del rival: boca abajo durante la partida, a la vista al terminar. */
sealed interface OpponentRack {
  data class Hidden(val tiles: Int) : OpponentRack
  data class Visible(val tiles: List<String>) : OpponentRack
}

/**
 * El reloj de un jugador. `remainingMs` negativo = se le acabo el tiempo principal y esta en el
 * descuento, del que le quedan `overtimeMs`.
 */
data class Clock(val remainingMs: Long, val overtimeMs: Long) {
  val inOvertime: Boolean get() = remainingMs < 0
}

enum class MoveType { PLACEMENT, PASS, EXCHANGE, INVALID }

/**
 * Una jugada ya hecha, para la planilla de movidas. `text` es la colocacion ("H8 CASA") o, en
 * un cambio, las fichas cambiadas (vacio si no se ven, como los cambios del bot); `tiles` es
 * cuantas se cambiaron. `myTotal` y `opponentTotal` son el marcador tras la jugada.
 */
data class Move(
  val side: Side,
  val type: MoveType,
  val text: String = "",
  val tiles: Int = 0,
  val points: Int = 0,
  val myTotal: Int,
  val opponentTotal: Int,
)

/** Fin de la partida. `winner` null = empate. */
data class GameEnd(val winner: Side?, val byTimeout: Boolean = false)

data class ClassicView(
  /** El alias del bot rival (uno de [BOTS]). */
  val opponent: String,
  val board: Board,
  /** Tus fichas: "A", "CH", "?"... */
  val rack: List<String>,
  val opponentRack: OpponentRack,
  val myScore: Int,
  val opponentScore: Int,
  /** De quien es el turno; null = partida terminada. */
  val turn: Side?,
  /** Relojes; null = partida sin tiempo. */
  val myClock: Clock? = null,
  val opponentClock: Clock? = null,
  /** Fichas en la bolsa. */
  val bag: Int,
  /** Las fichas que no ves: la bolsa mas el atril del rival. */
  val unseen: List<String>,
  /** false = la bolsa solo dice cuantas fichas quedan, no cuales. */
  val showUnseen: Boolean = true,
  val moves: List<Move>,
  val end: GameEnd? = null,
  /** Mensaje para el jugador (una jugada rechazada...); null = ninguno. */
  val notice: String? = null,
  /** En pausa: la partida se tapa hasta que el jugador continua. */
  val paused: Boolean = false,
) {
  /** Puntos de la ultima colocacion, la que se ve resaltada en el tablero. */
  val latestScore: Int? get() = moves.lastOrNull { it.type == MoveType.PLACEMENT }?.points
}

/** Lo que el jugador puede pedir desde la pantalla. */
interface ClassicActions {
  fun play(placement: Placement)
  fun exchange(tiles: List<String>)
  fun pass()
  fun resign()
  fun exit()
  /** Revisar la partida terminada. */
  fun analyze()
  fun pause()
  /** Seguir tras la pausa. */
  fun resume()
}
