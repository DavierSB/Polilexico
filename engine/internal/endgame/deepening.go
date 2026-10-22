package endgame

import (
	"errors"
	"slices"
	"time"

	"github.com/domino14/macondo/game"
	"github.com/domino14/macondo/move"
)

type Result struct {
	Move     *move.Move
	Spread   int
	Depth    int
	Exact    bool
	Playouts int
	Elapsed  time.Duration
}

func Search(g *game.Game, c Config) (Result, error) {
	pos, err := NewPosition(g)
	if err != nil {
		return Result{}, err
	}
	return SearchFrom(pos, c)
}

func SearchFrom(p *Position, c Config) (Result, error) {
	start := time.Now()
	s := &searcher{pos: p, config: c}
	s.setDeadline(start)
	r, err := s.deepen()
	r.Playouts, r.Elapsed = s.playouts, time.Since(start)
	return r, err
}

func newSearcher(g *game.Game, c Config) (*searcher, error) {
	pos, err := NewPosition(g)
	return &searcher{pos: pos, config: c}, err
}

func (s *searcher) deepen() (Result, error) {
	moves := s.rootMoves()
	best := Result{Move: moves[0]}
	for depth := 1; depth <= s.config.MaxDepth && !best.Exact; depth++ {
		r, err := s.root(moves, depth)
		if s.stopped(err) {
			return partial(best, r), nil
		}
		if err != nil {
			return best, err
		}
		best, moves = r, first(moves, r.Move)
	}
	return best, nil
}

func (s *searcher) root(moves []*move.Move, depth int) (Result, error) {
	n := node{player: s.pos.OnTurn(), depth: depth}
	s.truncated, s.estimated = false, false
	best := Result{Spread: -infinity, Depth: depth}
	for _, m := range moves {
		v, err := s.after(n, m, -infinity, -best.Spread)
		if err != nil {
			return best, err
		}
		if -v > best.Spread {
			best.Spread, best.Move = -v, m
		}
	}
	best.Exact = !s.truncated && !s.estimated && !s.rootNarrowed
	return best, nil
}

func (s *searcher) rootMoves() []*move.Move {
	all := s.cheapOrder(s.pos.Moves())
	moves := s.narrow(all, node{player: s.pos.OnTurn()})
	s.rootNarrowed = len(moves) < len(all)
	return moves
}

func (s *searcher) setDeadline(start time.Time) {
	if s.config.Budget > 0 {
		s.deadline = start.Add(s.config.Budget)
	}
}

func (s *searcher) stopped(err error) bool {
	return errors.Is(err, errTimeout) || s.cut
}

func partial(best, cut Result) Result {
	if best.Depth == 0 && cut.Move != nil {
		cut.Exact = false
		return cut
	}
	return best
}

func first(moves []*move.Move, m *move.Move) []*move.Move {
	i := slices.Index(moves, m)
	return slices.Concat([]*move.Move{m}, moves[:i], moves[i+1:])
}
