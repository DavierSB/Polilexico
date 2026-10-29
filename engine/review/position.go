package review

import (
	"lexico/engine/internal/core"
	"lexico/engine/internal/preendgame"
)

func FromPosition(board, rack string, myScore, opponentScore int) (*Turn, error) {
	rows, err := core.ParseBoard(board)
	if err != nil {
		return nil, err
	}
	if _, err := core.NewGameFromBoard(rows, rack); err != nil {
		return nil, err
	}
	t := &Turn{Number: 1, Player: "Tú", Rack: rackText(rack), Board: board, MyScore: myScore, OpponentScore: opponentScore,
		position: position{rack: rack, scores: [2]int{myScore, opponentScore}}}
	t.Endgame = t.bagEmpty() && t.unseen() > 0
	t.PreEndgame = preendgame.Applies(t.bag())
	t.drawBase = t.bag()
	return t, nil
}
