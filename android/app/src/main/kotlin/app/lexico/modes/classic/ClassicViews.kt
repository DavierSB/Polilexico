package app.lexico.modes.classic

import app.lexico.game.modes.classic.ClassicResult
import app.lexico.game.modes.classic.ClassicState
import app.lexico.game.modes.classic.MoveKind
import app.lexico.game.Outcome
import app.lexico.game.modes.classic.PlayedMove
import app.lexico.ui.classic.Clock
import app.lexico.ui.classic.ClassicView
import app.lexico.ui.classic.alias
import app.lexico.ui.classic.GameEnd
import app.lexico.ui.classic.Move
import app.lexico.ui.classic.MoveType
import app.lexico.ui.classic.OpponentRack
import app.lexico.ui.classic.Side

/** Lo que dibuja la pantalla clasica a partir del estado de la partida. */
fun classicView(state: ClassicState, opponent: String, notice: String?): ClassicView = ClassicView(
  opponent = alias(opponent), board = state.board, rack = state.rack, opponentRack = opponentRack(state),
  myScore = state.myScore, opponentScore = state.opponentScore, turn = turn(state),
  myClock = state.clocks?.let { Clock(it.myMs, it.myOvertimeMs) },
  opponentClock = state.clocks?.let { Clock(it.opponentMs, it.opponentOvertimeMs) },
  bag = state.bag, unseen = state.unseen, showUnseen = state.showUnseen,
  moves = state.moves.map(::move), end = state.result?.let(::gameEnd),
  notice = notice ?: state.botError, paused = state.paused,
)

/** Boca abajo mientras se juega; al terminar, lo que le quedo al rival. */
private fun opponentRack(state: ClassicState): OpponentRack =
  if (state.result == null) OpponentRack.Hidden(state.opponentTiles) else OpponentRack.Visible(state.opponentRack)

private fun turn(state: ClassicState): Side? = when {
  state.result != null -> null
  state.myTurn -> Side.ME
  else -> Side.OPPONENT
}

private fun move(m: PlayedMove): Move = Move(
  side = if (m.byMe) Side.ME else Side.OPPONENT, type = moveType(m.kind), text = m.text, tiles = m.tileCount,
  points = m.score, myTotal = m.myTotal, opponentTotal = m.opponentTotal,
)

private fun moveType(kind: MoveKind): MoveType = when (kind) {
  MoveKind.PLACEMENT -> MoveType.PLACEMENT
  MoveKind.PASS -> MoveType.PASS
  MoveKind.EXCHANGE -> MoveType.EXCHANGE
  MoveKind.INVALID -> MoveType.INVALID
}

private fun gameEnd(result: ClassicResult): GameEnd = GameEnd(winner = winner(result.outcome), byTimeout = result.lostOnTime)

private fun winner(outcome: Outcome): Side? = when (outcome) {
  Outcome.WIN -> Side.ME
  Outcome.LOSS -> Side.OPPONENT
  Outcome.TIE -> null
}
