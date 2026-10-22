package endgame

import (
	"errors"
	"sync/atomic"
	"time"

	"github.com/domino14/macondo/move"
)

const infinity = 1 << 20

var errTimeout = errors.New("tiempo agotado")

type node struct {
	player int
	depth  int
	ply    int
}

func (n node) child() node {
	return node{player: 1 - n.player, depth: n.depth - 1, ply: n.ply + 1}
}

type searcher struct {
	pos          *Position
	config       Config
	deadline     time.Time
	playouts     int
	truncated    bool
	estimated    bool
	rootNarrowed bool
	cut          bool
	lines        [][]*move.Move
	halt         *atomic.Bool
}

func (s *searcher) value(n node, alpha, beta int) (int, error) {
	if s.pos.Over() {
		s.endLine(n.ply)
		return s.pos.Spread(n.player), nil
	}
	if s.pos.Forced() {
		return s.forcedPass(n, alpha, beta)
	}
	if err := s.checkTime(); err != nil {
		return 0, err
	}
	if n.depth == 0 {
		return s.leaf(n), nil
	}
	return s.best(n, s.candidates(n), alpha, beta)
}

func (s *searcher) best(n node, moves []*move.Move, alpha, beta int) (int, error) {
	best := -infinity
	for _, m := range moves {
		v, err := s.after(n, m, -beta, -alpha)
		if err != nil {
			return 0, err
		}
		if -v > best {
			best = -v
			s.extendLine(n.ply, m)
		}
		alpha = max(alpha, best)
		if alpha >= beta {
			break
		}
	}
	return best, nil
}

func (s *searcher) after(n node, m *move.Move, alpha, beta int) (int, error) {
	if err := s.pos.Play(m); err != nil {
		return 0, err
	}
	defer s.pos.Undo()
	return s.value(n.child(), alpha, beta)
}

func (s *searcher) leaf(n node) int {
	s.playouts++
	s.estimated = true
	if s.lines == nil {
		return s.pos.Playout(n.player)
	}
	line, spread := s.pos.PlayoutLine(n.player)
	s.lines[n.ply] = line
	return spread
}

func (s *searcher) checkTime() error {
	if s.expired() {
		return errTimeout
	}
	return nil
}

func (s *searcher) expired() bool {
	return s.halted() || !s.deadline.IsZero() && time.Now().After(s.deadline)
}

func (s *searcher) halted() bool {
	return s.halt != nil && s.halt.Load()
}
