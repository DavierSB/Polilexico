package endgame

import (
	"github.com/domino14/macondo/game"
	"github.com/domino14/macondo/move"
	"github.com/domino14/word-golib/tilemapping"
)

const (
	fnvOffset uint64 = 14695981039346656037
	fnvPrime  uint64 = 1099511628211
)

func (p *Position) Forced() bool {
	return p.RackSize(1-p.onTurn) > game.RackTileLimit
}

func (p *Position) allowed(moves []*move.Move) []*move.Move {
	if p.RackSize(p.onTurn) <= game.RackTileLimit {
		return moves
	}
	var kept []*move.Move
	for _, m := range moves {
		if m.Action() == move.MoveTypePlay && m.TilesPlayed() <= game.RackTileLimit {
			kept = append(kept, m)
		}
	}
	if len(kept) == 0 {
		return []*move.Move{p.pass()}
	}
	return kept
}

func (p *Position) pass() *move.Move {
	return move.NewPassMove(p.rack(p.onTurn).TilesOn(), p.ld.TileMapping())
}

func (s *searcher) forcedPass(n node, alpha, beta int) (int, error) {
	if err := s.pos.Play(s.pos.pass()); err != nil {
		return 0, err
	}
	defer s.pos.Undo()
	v, err := s.value(node{player: 1 - n.player, depth: n.depth, ply: n.ply}, -beta, -alpha)
	return -v, err
}

func (p *Position) Key() uint64 {
	h := fnvOffset
	for _, sq := range p.board.GetSquares() {
		h = (h ^ uint64(sq)) * fnvPrime
	}
	for player, r := range p.racks {
		for _, n := range r.LetArr {
			h = (h ^ uint64(n)) * fnvPrime
		}
		h = (h ^ uint64(p.points[player])) * fnvPrime
	}
	return h
}

func (p *Position) FaceValue(m *move.Move) int {
	value := 0
	for _, t := range m.Tiles() {
		if t != 0 && !t.IsBlanked() {
			value += p.ld.Score(t)
		}
	}
	return value
}

func (p *Position) SetRack(player int, r *tilemapping.Rack) {
	p.racks[player].CopyFrom(r)
}
