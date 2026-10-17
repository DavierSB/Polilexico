package endgame

import (
	"cmp"
	"slices"

	"github.com/domino14/macondo/move"
	"github.com/domino14/word-golib/tilemapping"
)

const perCriterion = 8

type guests struct {
	threats []threat
	spots   map[tilemapping.MachineLetter]squares
	hard    uint64
}

type threat struct {
	value   int
	squares squares
}

type criterion func(m *move.Move) int

func (s *searcher) invited(moves []*move.Move) []*move.Move {
	var picked []*move.Move
	for _, c := range s.pos.invitations(s.pos.guests()) {
		picked = union(picked, topBy(moves, c))
	}
	return picked
}

func (p *Position) guests() guests {
	me := p.OnTurn()
	return guests{threats: p.allThreats(1 - me), spots: p.spots(1 - me), hard: p.stuckLetters(me)}
}

func (p *Position) invitations(g guests) []criterion {
	return []criterion{
		func(m *move.Move) int { return g.blocking(m) },
		func(m *move.Move) int { return p.trapping(m, g) },
		func(m *move.Move) int { return p.hardPlayed(m, g.hard) },
		yield,
	}
}

func (p *Position) allThreats(opponent int) []threat {
	p.gen.GenAll(p.rack(opponent), false)
	all := make([]threat, 0, len(p.gen.Plays()))
	for _, m := range p.gen.Plays() {
		all = append(all, threat{value: p.valueFor(m, opponent), squares: squaresOf(m)})
	}
	slices.SortStableFunc(all, func(a, b threat) int { return cmp.Compare(b.value, a.value) })
	return all
}

func (g guests) blocking(m *move.Move) int {
	mine := squaresOf(m)
	for _, t := range g.threats {
		if !mine.overlaps(t.squares) {
			return g.threats[0].value - t.value
		}
	}
	return 0
}

func (p *Position) trapping(m *move.Move, g guests) int {
	mine, rack, value := squaresOf(m), p.rack(1-p.OnTurn()), 0
	for ml, spots := range g.spots {
		if spots.within(mine) {
			value += rack.LetArr[ml] * p.ld.Score(ml)
		}
	}
	return value
}

func (p *Position) hardPlayed(m *move.Move, hard uint64) int {
	value := 0
	for _, t := range m.Tiles() {
		if t != 0 && !t.IsBlanked() && hard&(1<<uint(t)) != 0 {
			value += p.ld.Score(t)
		}
	}
	return value
}

func yield(m *move.Move) int {
	if m.TilesPlayed() == 0 {
		return 0
	}
	return 100 * m.Score() / m.TilesPlayed()
}

func topBy(moves []*move.Move, value criterion) []*move.Move {
	var pairs []scored
	for _, m := range moves {
		if v := value(m); v > 0 {
			pairs = append(pairs, scored{m, v})
		}
	}
	slices.SortStableFunc(pairs, func(a, b scored) int { return cmp.Compare(b.value, a.value) })
	return movesOf(pairs[:min(len(pairs), perCriterion)])
}

func union(into, more []*move.Move) []*move.Move {
	for _, m := range more {
		if !slices.Contains(into, m) {
			into = append(into, m)
		}
	}
	return into
}
