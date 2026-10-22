package preendgame

import (
	"cmp"
	"errors"
	"slices"
	"time"

	"github.com/domino14/macondo/game"
	"github.com/domino14/macondo/move"

	"lexico/engine/internal/endgame"
)

var errScanTimeout = errors.New("fase 1 sin terminar")

type scanner struct {
	pos      *endgame.Position
	me       int
	deadline time.Time
	seen     map[uint64]bool
}

func newScanner(pos *endgame.Position, me int, budget time.Duration) *scanner {
	return &scanner{pos: pos, me: me, deadline: time.Now().Add(budget), seen: map[uint64]bool{}}
}

func (s *scanner) reachable() (bool, error) {
	if time.Now().After(s.deadline) {
		return false, errScanTimeout
	}
	moves := s.keeping()
	if s.ceiling(moves) < s.needed() {
		return false, nil
	}
	for _, m := range moves {
		found, err := s.after(m)
		if err != nil || found {
			return found, err
		}
	}
	return false, nil
}

func (s *scanner) after(m *move.Move) (bool, error) {
	if err := s.pos.Play(m); err != nil {
		return false, err
	}
	defer s.pos.Undo()
	if s.rivalAhead() {
		return true, nil
	}
	if s.visited() {
		return false, nil
	}
	if err := s.pos.Play(s.pos.Moves()[0]); err != nil {
		return false, err
	}
	defer s.pos.Undo()
	return s.reachable()
}

func (s *scanner) visited() bool {
	key := s.pos.Key()
	if s.seen[key] {
		return true
	}
	s.seen[key] = true
	return false
}

func (s *scanner) keeping() []*move.Move {
	size := s.pos.RackSize(s.pos.OnTurn())
	var kept []*move.Move
	for _, m := range s.pos.Moves() {
		if m.Action() == move.MoveTypePlay && size-m.TilesPlayed() > game.RackTileLimit {
			kept = append(kept, m)
		}
	}
	slices.SortStableFunc(kept, func(a, b *move.Move) int { return cmp.Compare(s.gain(b), s.gain(a)) })
	return kept
}

func (s *scanner) gain(m *move.Move) int {
	return m.Score() + s.pos.FaceValue(m)
}

func (s *scanner) rivalAhead() bool {
	return s.needed() <= 0
}

func (s *scanner) needed() int {
	rival := 1 - s.me
	return -(s.pos.Spread(rival) - s.pos.RackValue(rival) + s.pos.RackValue(s.me))
}

func (s *scanner) ceiling(moves []*move.Move) int {
	budget := s.pos.RackSize(1-s.me) - game.RackTileLimit - 1
	best := make([]int, budget+1)
	for _, m := range moves {
		best[m.TilesPlayed()] = max(best[m.TilesPlayed()], s.gain(m))
	}
	return knapsack(best)
}

func knapsack(best []int) int {
	total := make([]int, len(best))
	for t := 1; t < len(best); t++ {
		total[t] = total[t-1]
		for j := 1; j <= t; j++ {
			total[t] = max(total[t], total[t-j]+best[j])
		}
	}
	return total[len(best)-1]
}
