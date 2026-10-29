package preendgame

import (
	"github.com/domino14/macondo/game"
	"github.com/domino14/macondo/move"
)

func (b *preBot) shares(m *move.Move, s situation) (float64, float64) {
	ahead := func() bool { return b.aheadAfter(m, s, 0) }
	escapes := func() bool { return !b.aheadAfter(m, s, b.rivalSingle(m, s)) }
	return split(bagAfter(m, s.bag), drawOdds(m, s.bag, s.drawBase), ahead, escapes)
}

func split(after int, odds float64, ahead, escapes func() bool) (float64, float64) {
	switch after {
	case 0:
		return odds, 1 - odds
	case 1:
		return lastTile(odds, ahead())
	case 2:
		return lastTwo(odds, ahead, escapes)
	}
	return odds, 0
}

func lastTile(odds float64, ahead bool) (float64, float64) {
	if ahead {
		return odds, 1 - odds
	}
	return 1, 0
}

func lastTwo(odds float64, ahead, escapes func() bool) (float64, float64) {
	switch {
	case !ahead():
		return odds, 0
	case escapes():
		return 1, 0
	}
	return odds, 1 - odds
}

func (b *preBot) aheadAfter(m *move.Move, s situation, rivalGain int) bool {
	g := b.game
	me, ld := g.PlayerOnTurn(), g.Rules().LetterDistribution()
	mine := g.PointsFor(me) + m.Score() - valueOf(m.Leave(), ld) - highest(s.unseen, ld, drawn(m, s.bag))
	theirs := g.PointsFor(1-me) + rivalGain - lowest(s.unseen, ld, game.RackTileLimit)
	return mine > theirs
}

func (b *preBot) rivalSingle(m *move.Move, s situation) int {
	return b.spots.bestSingle(s.board, m, b.letters.singles(s.unseen))
}
