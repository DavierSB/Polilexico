package endgame

import (
	"cmp"
	"slices"

	"github.com/domino14/macondo/move"
)

type scored struct {
	move  *move.Move
	value int
}

func (s *searcher) candidates(n node) []*move.Move {
	return s.narrow(s.cheapOrder(s.pos.Moves()), n)
}

func (s *searcher) narrow(moves []*move.Move, n node) []*move.Move {
	l := s.config.limits(n.ply, len(moves))
	if l.width == 0 || len(moves) <= l.width {
		return moves
	}
	s.truncated = true
	kept := s.playoutOrder(s.prefiltered(moves, l), n.player)[:l.width]
	if l.wide {
		return s.withForced(kept, moves, l.width)
	}
	return kept
}

func (s *searcher) prefiltered(moves []*move.Move, l limits) []*move.Move {
	base := moves[:min(len(moves), l.prefilter)]
	if len(moves) > len(base) {
		return union(slices.Clip(base), s.invited(moves))
	}
	return base
}

func (s *searcher) withForced(kept, all []*move.Move, limit int) []*move.Move {
	kept = slices.Clip(kept)
	added := 0
	for _, m := range all {
		if added < limit && s.forced(m) && !slices.Contains(kept, m) {
			kept = append(kept, m)
			added++
		}
	}
	return kept
}

func (s *searcher) forced(m *move.Move) bool {
	return m.Action() == move.MoveTypePass || m.TilesPlayed() == s.pos.RackSize(s.pos.OnTurn())
}

func (s *searcher) cheapOrder(moves []*move.Move) []*move.Move {
	return sortedBy(moves, func(m *move.Move) int { return s.pos.estimate(m) })
}

func (s *searcher) playoutOrder(moves []*move.Move, player int) []*move.Move {
	return sortedBy(moves, func(m *move.Move) int { return s.note(m, player) })
}

func (s *searcher) note(m *move.Move, player int) int {
	if s.expired() {
		s.cut = true
		return -infinity
	}
	if err := s.pos.Play(m); err != nil {
		return -infinity
	}
	defer s.pos.Undo()
	s.playouts++
	return s.pos.Playout(player)
}

func sortedBy(moves []*move.Move, value func(*move.Move) int) []*move.Move {
	pairs := make([]scored, len(moves))
	for i, m := range moves {
		pairs[i] = scored{m, value(m)}
	}
	slices.SortStableFunc(pairs, func(a, b scored) int { return cmp.Compare(b.value, a.value) })
	return movesOf(pairs)
}

func movesOf(pairs []scored) []*move.Move {
	out := make([]*move.Move, len(pairs))
	for i, p := range pairs {
		out[i] = p.move
	}
	return out
}
