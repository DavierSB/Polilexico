package preendgame

import (
	"slices"

	"github.com/domino14/macondo/game"
	"github.com/domino14/macondo/move"
)

const allPlays = 1 << 20

type Verdict int

const (
	Plain Verdict = iota
	BingoWins
	PassWins
)

type Rated struct {
	Move       *move.Move
	Adjustment float64
	Verdict    Verdict
}

func (r Rated) Value() float64 {
	return r.Move.Equity() + r.Adjustment
}

type Analysis struct {
	bot       *preBot
	situation situation
	plays     []*move.Move
}

func (a *Analysis) Ranking(count int) []Rated {
	bonus := a.bot.maxBonus(a.situation)
	var top []Rated
	for _, m := range a.all() {
		if len(top) == count && m.Equity()+bonus <= top[count-1].Value() {
			break
		}
		top = insert(top, a.rate(m), count)
	}
	return top
}

func (a *Analysis) Find(keep func(*move.Move) bool) *Rated {
	for _, m := range a.all() {
		if keep(m) {
			r := a.rate(m)
			return &r
		}
	}
	return nil
}

func (a *Analysis) Verdict() *Rated {
	m := a.bot.bingoOrPass(a.situation, a.all())
	if m == nil {
		return nil
	}
	r := a.rate(m)
	r.Verdict = verdictOf(m)
	return &r
}

func (a *Analysis) best() *move.Move {
	if a.situation.where == onBoard {
		return a.bot.hasty.GenerateMoves(1)[0]
	}
	if v := a.Verdict(); v != nil {
		return v.Move
	}
	return a.Ranking(1)[0].Move
}

func (a *Analysis) all() []*move.Move {
	if a.plays == nil {
		a.plays = a.bot.hasty.GenerateMoves(allPlays)
	}
	return a.plays
}

func (a *Analysis) rate(m *move.Move) Rated {
	return Rated{Move: m, Adjustment: a.bot.adjustment(m, a.situation)}
}

func (b *preBot) maxBonus(s situation) float64 {
	if s.where == mine || s.where == unseen && s.bag <= game.RackTileLimit+2 {
		return b.config.Penalty
	}
	return 0
}

func insert(top []Rated, r Rated, count int) []Rated {
	i := len(top)
	for i > 0 && r.Value() > top[i-1].Value() {
		i--
	}
	if i >= count {
		return top
	}
	top = slices.Insert(top, i, r)
	return top[:min(len(top), count)]
}

func verdictOf(m *move.Move) Verdict {
	if m.Action() == move.MoveTypePass {
		return PassWins
	}
	return BingoWins
}
