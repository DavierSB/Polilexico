package preendgame

import (
	"github.com/domino14/macondo/game"
	"github.com/domino14/macondo/move"

	"lexico/engine/internal/core"
	"lexico/engine/internal/endgame"
)

func bestBingo(plays []*move.Move) *move.Move {
	var best *move.Move
	for _, m := range plays {
		if m.Action() == move.MoveTypePlay && m.TilesPlayed() == game.RackTileLimit && (best == nil || m.Score() > best.Score()) {
			best = m
		}
	}
	return best
}

func (b *preBot) bingoWins(s situation, m *move.Move) bool {
	g := b.game
	me := g.PlayerOnTurn()
	pos, err := afterBingo(g, me, m)
	if err != nil {
		return false
	}
	deals, err := b.deals(s.unseen, s.bag)
	if err != nil || len(deals) == 0 {
		return false
	}
	worstDeal(pos, me, deals).apply(pos, me)
	r, err := endgame.SearchFrom(pos, b.config.Endgame)
	return err == nil && r.Move != nil && r.Spread < 0
}

func afterBingo(g *game.Game, me int, m *move.Move) (*endgame.Position, error) {
	crosses, err := core.CrossSets()
	if err != nil {
		return nil, err
	}
	var s endgame.State
	s.Board, s.OnTurn = g.Board().Copy(), 1-me
	s.Board.PlayMove(m)
	crosses.UpdateForMove(s.Board, m)
	s.Racks[me], s.Racks[1-me] = g.RackFor(me), g.RackFor(1-me)
	s.Points = [2]int{g.PointsFor(0), g.PointsFor(1)}
	s.Points[me] += m.Score()
	return endgame.FromState(s)
}

func worstDeal(pos *endgame.Position, me int, deals []deal) deal {
	worst, worstValue := deals[0], 0
	for i, d := range deals {
		d.apply(pos, me)
		if v := pos.Playout(me); i == 0 || v < worstValue {
			worst, worstValue = d, v
		}
	}
	return worst
}
