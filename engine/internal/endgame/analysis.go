package endgame

import (
	"cmp"
	"slices"
	"time"

	"github.com/domino14/macondo/game"
	"github.com/domino14/macondo/move"
)

type Line struct {
	Move   *move.Move
	Spread int
	Reply  []*move.Move
}

type Analysis struct {
	Lines []Line
	Depth int
	Exact bool
}

func Analyze(g *game.Game, c Config, count int, keep func(*move.Move) bool) (Analysis, error) {
	start := time.Now()
	s, err := newSearcher(g, c)
	if err != nil {
		return Analysis{}, err
	}
	s.trackLines()
	a, err := s.level(s.analyzed(count, keep), 1)
	if err != nil {
		return a, err
	}
	s.setDeadline(start)
	return s.deepenAll(a)
}

func (s *searcher) deepenAll(a Analysis) (Analysis, error) {
	for depth := 2; depth <= s.config.MaxDepth && !a.Exact; depth++ {
		next, err := s.level(a.moves(), depth)
		if s.stopped(err) {
			break
		}
		if err != nil {
			return a, err
		}
		a = next
	}
	return a, nil
}

func (s *searcher) level(moves []*move.Move, depth int) (Analysis, error) {
	n := node{player: s.pos.OnTurn(), depth: depth}
	s.truncated, s.estimated = false, false
	lines := make([]Line, 0, len(moves))
	for _, m := range moves {
		v, err := s.after(n, m, -infinity, infinity)
		if err != nil {
			return Analysis{}, err
		}
		lines = append(lines, Line{Move: m, Spread: -v, Reply: s.lines[1]})
	}
	slices.SortStableFunc(lines, func(a, b Line) int { return cmp.Compare(b.Spread, a.Spread) })
	return Analysis{Lines: lines, Depth: depth, Exact: !s.truncated && !s.estimated}, nil
}

func (s *searcher) analyzed(count int, keep func(*move.Move) bool) []*move.Move {
	all := s.cheapOrder(s.pos.Moves())
	ranked := s.playoutOrder(s.narrow(all, node{player: s.pos.OnTurn()}), s.pos.OnTurn())
	picked := slices.Clip(ranked[:min(count, len(ranked))])
	return union(picked, matching(all, keep))
}

func (a Analysis) moves() []*move.Move {
	out := make([]*move.Move, len(a.Lines))
	for i, l := range a.Lines {
		out[i] = l.Move
	}
	return out
}

func matching(moves []*move.Move, keep func(*move.Move) bool) []*move.Move {
	var out []*move.Move
	for _, m := range moves {
		if keep != nil && keep(m) {
			out = append(out, m)
		}
	}
	return out
}
