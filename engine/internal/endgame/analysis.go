package endgame

import (
	"cmp"
	"slices"
	"sync"
	"sync/atomic"
	"time"

	"github.com/domino14/macondo/game"
	"github.com/domino14/macondo/move"
)

type Line struct {
	Move   *move.Move
	Spread int
	Reply  []*move.Move
	Depth  int
}

type Analysis struct {
	Lines []Line
	Depth int
	Exact bool
}

type Analyzer struct {
	s      *searcher
	halt   atomic.Bool
	mu     sync.Mutex
	latest Analysis
}

func Analyze(g *game.Game, c Config, count int, keep func(*move.Move) bool) (Analysis, error) {
	z, err := NewAnalyzer(g, c)
	if err != nil {
		return Analysis{}, err
	}
	return z.Run(count, keep, func() {})
}

func NewAnalyzer(g *game.Game, c Config) (*Analyzer, error) {
	s, err := newSearcher(g, c)
	if err != nil {
		return nil, err
	}
	z := &Analyzer{s: s}
	s.halt = &z.halt
	s.trackLines()
	return z, nil
}

func (z *Analyzer) Run(count int, keep func(*move.Move) bool, changed func()) (Analysis, error) {
	start := time.Now()
	a := pending(z.s.analyzed(count, keep))
	z.publish(a, changed)
	for depth := 1; depth <= z.s.config.MaxDepth && !a.Exact; depth++ {
		next, err := z.s.level(a, depth, func(p Analysis) { z.publish(p, changed) })
		if z.s.stopped(err) {
			return a, nil
		}
		if err != nil {
			return a, err
		}
		a = next
		z.publish(a, changed)
		z.s.setDeadline(start)
	}
	return a, nil
}

func (z *Analyzer) Stop() {
	z.halt.Store(true)
}

func (z *Analyzer) Latest() Analysis {
	z.mu.Lock()
	defer z.mu.Unlock()
	return z.latest
}

func (z *Analyzer) publish(a Analysis, changed func()) {
	z.mu.Lock()
	z.latest = a
	z.mu.Unlock()
	changed()
}

func (s *searcher) level(prev Analysis, depth int, progress func(Analysis)) (Analysis, error) {
	n := node{player: s.pos.OnTurn(), depth: depth}
	s.truncated, s.estimated = false, false
	lines := slices.Clone(prev.Lines)
	for i, l := range prev.Lines {
		v, err := s.after(n, l.Move, -infinity, infinity)
		if err != nil {
			return Analysis{}, err
		}
		lines[i] = Line{Move: l.Move, Spread: -v, Reply: s.lines[1], Depth: depth}
		progress(sorted(lines, depth, false))
	}
	return sorted(lines, depth, !s.truncated && !s.estimated), nil
}

func (s *searcher) analyzed(count int, keep func(*move.Move) bool) []*move.Move {
	all := s.cheapOrder(s.pos.Moves())
	ranked := s.playoutOrder(s.narrow(all, node{player: s.pos.OnTurn()}), s.pos.OnTurn())
	picked := slices.Clip(ranked[:min(count, len(ranked))])
	return union(picked, matching(all, keep))
}

func pending(moves []*move.Move) Analysis {
	lines := make([]Line, len(moves))
	for i, m := range moves {
		lines[i] = Line{Move: m}
	}
	return Analysis{Lines: lines}
}

func sorted(lines []Line, depth int, exact bool) Analysis {
	out := slices.Clone(lines)
	slices.SortStableFunc(out, byValue)
	return Analysis{Lines: out, Depth: depth, Exact: exact}
}

func byValue(a, b Line) int {
	if (a.Depth > 0) != (b.Depth > 0) {
		return cmp.Compare(b.Depth, a.Depth)
	}
	return cmp.Compare(b.Spread, a.Spread)
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
