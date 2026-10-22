package preendgame

import "github.com/domino14/macondo/move"

const allPlays = 1 << 20

func (b *preBot) choose(top *move.Move, plays []*move.Move, s situation) *move.Move {
	best, bestValue := top, top.Equity()+b.adjustment(top, s)
	bonus := b.maxBonus(s)
	for _, m := range plays {
		if m.Equity()+bonus <= bestValue {
			break
		}
		if v := m.Equity() + b.adjustment(m, s); v > bestValue {
			best, bestValue = m, v
		}
	}
	return best
}

func (b *preBot) maxBonus(s situation) float64 {
	if s.where == mine {
		return b.config.Penalty
	}
	return 0
}
