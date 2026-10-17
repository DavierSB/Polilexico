package endgame

import (
	"slices"

	"github.com/domino14/macondo/game"
	pb "github.com/domino14/macondo/gen/api/proto/macondo"
	"github.com/domino14/macondo/move"
	"github.com/domino14/macondo/movegen"
	"github.com/domino14/word-golib/tilemapping"

	"lexico/engine/internal/core"
)

const stackSize = 96

type Position struct {
	game *game.Game
	gen  *movegen.GordonGenerator
	ld   *tilemapping.LetterDistribution
}

func NewPosition(g *game.Game) (*Position, error) {
	gc := g.Copy()
	gc.SetBackupMode(game.SimulationMode)
	gc.SetStateStackLength(stackSize)
	gc.SetEndgameMode(true)
	gen, err := core.NewGenerator(gc.Board())
	if err != nil {
		return nil, err
	}
	gen.SetGenPass(true)
	return &Position{game: gc, gen: gen, ld: gc.Rules().LetterDistribution()}, nil
}

func (p *Position) Moves() []*move.Move {
	p.gen.GenAll(p.rack(p.OnTurn()), false)
	return slices.Clone(p.gen.Plays())
}

func (p *Position) Play(m *move.Move) error {
	return p.game.PlayMove(m, false, 0)
}

func (p *Position) Undo() {
	p.game.UnplayLastMove()
}

func (p *Position) Over() bool {
	return p.game.Playing() == pb.PlayState_GAME_OVER
}

func (p *Position) OnTurn() int {
	return p.game.PlayerOnTurn()
}

func (p *Position) Spread(player int) int {
	return p.game.SpreadFor(player)
}

func (p *Position) RackValue(player int) int {
	return p.rack(player).ScoreOn(p.ld)
}

func (p *Position) RackSize(player int) int {
	return int(p.rack(player).NumTiles())
}

func (p *Position) rack(player int) *tilemapping.Rack {
	return p.game.RackFor(player)
}
