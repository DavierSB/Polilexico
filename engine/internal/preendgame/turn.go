package preendgame

import (
	"github.com/domino14/macondo/game"
	pb "github.com/domino14/macondo/gen/api/proto/macondo"
	"github.com/domino14/macondo/move"

	"lexico/engine/internal/core"
)

const reach = 2 * game.RackTileLimit

type Turn struct {
	game     *game.Game
	drawBase int
}

func Prepare(g *game.Game) *Turn {
	bag := g.Bag().TilesRemaining()
	if !Applies(bag) {
		return nil
	}
	return PrepareFrom(g, drawBase(g, bag))
}

func Applies(bag int) bool {
	return bag > 0 && bag <= reach
}

func PrepareFrom(g *game.Game, drawBase int) *Turn {
	return &Turn{game: g.Copy(), drawBase: drawBase}
}

func (t *Turn) Move(c Config) (*move.Move, error) {
	a, err := t.Analyze(c)
	if err != nil {
		return nil, err
	}
	return core.CopyMove(a.best()), nil
}

func (t *Turn) Analyze(c Config) (*Analysis, error) {
	b, err := newBot(t.game, c)
	if err != nil {
		return nil, err
	}
	return &Analysis{bot: b, situation: b.situation(t.drawBase)}, nil
}

func drawBase(g *game.Game, bag int) int {
	last := g.LastEvent()
	if last != nil && last.GetType() == pb.GameEvent_TILE_PLACEMENT_MOVE {
		return bag + int(last.GetNumTilesFromRack())
	}
	return bag
}
