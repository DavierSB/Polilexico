package app.lexico.game.modes.classic

import app.lexico.game.Outcome

internal fun ClassicState.flipped(): ClassicState = copy(
  rack = opponentRack, opponentRack = rack, opponentTiles = rack.size,
  myScore = opponentScore, opponentScore = myScore, myTurn = !myTurn,
  moves = moves.map { it.flipped() }, clocks = clocks?.flipped(), result = result?.flipped(), flipped = !flipped,
)

internal fun ClassicClocks.flipped(): ClassicClocks = ClassicClocks(
  opponentMs, opponentOvertimeMs, myMs, myOvertimeMs, running?.let { if (it == Side.ME) Side.OPPONENT else Side.ME },
)

private fun ClassicResult.flipped(): ClassicResult = copy(
  outcome = outcome.reversed(), lostOnTime = rivalLostOnTime, rivalLostOnTime = lostOnTime,
  ending = ending?.let { it.copy(myDelta = it.opponentDelta, opponentDelta = it.myDelta) },
  myTimePenalty = opponentTimePenalty, opponentTimePenalty = myTimePenalty,
)

private fun PlayedMove.flipped(): PlayedMove = copy(byMe = !byMe, myTotal = opponentTotal, opponentTotal = myTotal)
