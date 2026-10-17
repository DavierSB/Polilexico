package classic

import (
	"slices"

	"github.com/domino14/macondo/game"

	"lexico/engine/internal/core"
	"lexico/engine/internal/endgame"
)

var endgameBots = []string{core.DefaultBot, "BetterBot"}

func (c *Game) endgameSnapshot() *game.Game {
	if !slices.Contains(endgameBots, c.botName) || c.g.Bag().TilesRemaining() > 0 {
		return nil
	}
	return c.g.Copy()
}

func (b *botChoice) searchEndgame(g *game.Game) {
	if r, err := endgame.Search(g, endgame.Default); err == nil && r.Move != nil {
		b.move = core.CopyMove(r.Move)
	}
}
