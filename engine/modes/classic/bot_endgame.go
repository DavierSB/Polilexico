package classic

import (
	"slices"

	"github.com/domino14/macondo/game"

	"lexico/engine/internal/core"
	"lexico/engine/internal/endgame"
	"lexico/engine/internal/preendgame"
)

var endgameBots = []string{core.DefaultBot, "BetterBot"}

var preEndgameBots = []string{core.DefaultBot, "BetterBot"}

func (b *botChoice) think() {
	switch {
	case b.endgame != nil:
		b.searchEndgame(b.endgame)
	case b.preEndgame != nil:
		b.searchPreEndgame(b.preEndgame)
	}
}

func (c *Game) endgameSnapshot() *game.Game {
	if !slices.Contains(endgameBots, c.botName) || c.g.Bag().TilesRemaining() > 0 {
		return nil
	}
	return c.g.Copy()
}

func (c *Game) preEndgameTurn() *preendgame.Turn {
	if !slices.Contains(preEndgameBots, c.botName) {
		return nil
	}
	return preendgame.Prepare(c.g)
}

func (b *botChoice) searchEndgame(g *game.Game) {
	if r, err := endgame.Search(g, endgame.Default); err == nil && r.Move != nil {
		b.move = core.CopyMove(r.Move)
	}
}

func (b *botChoice) searchPreEndgame(t *preendgame.Turn) {
	if m, err := t.Move(preendgame.Default); err == nil && m != nil {
		b.move = m
	}
}
