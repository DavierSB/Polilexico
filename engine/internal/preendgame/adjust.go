package preendgame

import (
	"slices"

	"github.com/domino14/macondo/game"
	"github.com/domino14/macondo/move"
	"github.com/domino14/word-golib/tilemapping"
)

func (b *preBot) adjustment(m *move.Move, s situation) float64 {
	if bagAfter(m, s.bag) > game.RackTileLimit {
		return 0
	}
	switch s.where {
	case mine:
		return b.holdingAdjustment(m, s)
	case unseen:
		return b.unseenAdjustment(m, s)
	}
	return 0
}

func (b *preBot) holdingAdjustment(m *move.Move, s situation) float64 {
	switch {
	case b.letters.plays(m):
		return 0
	case b.letters.exchanges(m):
		return b.config.Penalty * flag(!b.letters.partnered(returned(s.unseen, m.Tiles())))
	}
	return -b.config.Penalty * flag(!b.spots.canPlace(s.board, m, m.Leave()))
}

func (b *preBot) unseenAdjustment(m *move.Move, s situation) float64 {
	odds := drawOdds(m, s.drawBase)
	if odds == 0 || b.spots.canPlace(s.board, m, append(slices.Clone(m.Leave()), b.letters.q)) {
		return 0
	}
	return -b.config.Penalty * odds
}

func drawOdds(m *move.Move, base int) float64 {
	return min(1, float64(drawn(m))/float64(max(base, 1)))
}

func bagAfter(m *move.Move, bag int) int {
	if m.Action() == move.MoveTypePlay {
		return max(bag-m.TilesPlayed(), 0)
	}
	return bag
}

func drawn(m *move.Move) int {
	switch m.Action() {
	case move.MoveTypePlay:
		return m.TilesPlayed()
	case move.MoveTypeExchange:
		return len(m.Tiles())
	}
	return 0
}

func returned(r *tilemapping.Rack, tiles tilemapping.MachineWord) *tilemapping.Rack {
	c := r.Copy()
	for _, t := range tiles {
		c.Add(t)
	}
	return c
}

func flag(b bool) float64 {
	if b {
		return 1
	}
	return 0
}
