package endgame

import (
	"errors"
	"slices"

	"github.com/domino14/macondo/board"
	"github.com/domino14/macondo/cross_set"
	"github.com/domino14/macondo/game"
	"github.com/domino14/macondo/move"
	"github.com/domino14/macondo/movegen"
	"github.com/domino14/word-golib/tilemapping"

	"lexico/engine/internal/core"
)

const maxScoreless = 2

var errMoveType = errors.New("en el final solo se juegan fichas o se pasa")

type Position struct {
	board     *board.GameBoard
	racks     [2]*tilemapping.Rack
	points    [2]int
	onTurn    int
	scoreless int
	over      bool
	stack     []snapshot
	depth     int
	crosses   cross_set.Generator
	gen       *movegen.GordonGenerator
	ld        *tilemapping.LetterDistribution
}

type State struct {
	Board     *board.GameBoard
	Racks     [2]*tilemapping.Rack
	Points    [2]int
	OnTurn    int
	Scoreless int
}

func NewPosition(g *game.Game) (*Position, error) {
	gc := g.Copy()
	gc.SetEndgameMode(true)
	return FromState(State{
		Board:     gc.Board(),
		Racks:     [2]*tilemapping.Rack{gc.RackFor(0), gc.RackFor(1)},
		Points:    [2]int{gc.PointsFor(0), gc.PointsFor(1)},
		OnTurn:    gc.PlayerOnTurn(),
		Scoreless: gc.ScorelessTurns(),
	})
}

func FromState(s State) (*Position, error) {
	crosses, err := core.CrossSets()
	if err != nil {
		return nil, err
	}
	p := &Position{board: s.Board.Copy(), racks: [2]*tilemapping.Rack{s.Racks[0].Copy(), s.Racks[1].Copy()},
		points: s.Points, onTurn: s.OnTurn, scoreless: s.Scoreless, crosses: crosses}
	if p.gen, err = core.NewGenerator(p.board); err != nil {
		return nil, err
	}
	p.gen.SetGenPass(true)
	p.gen.SetMaxTileUsage(game.RackTileLimit)
	p.ld, err = core.Distribution()
	return p, err
}

func (p *Position) Moves() []*move.Move {
	if p.Forced() {
		return []*move.Move{p.pass()}
	}
	p.gen.GenAll(p.rack(p.onTurn), false)
	return slices.Clone(p.allowed(p.gen.Plays()))
}

func (p *Position) Play(m *move.Move) error {
	p.push()
	switch m.Action() {
	case move.MoveTypePlay:
		p.place(m)
	case move.MoveTypePass:
		p.passTurn()
	default:
		p.Undo()
		return errMoveType
	}
	p.onTurn = 1 - p.onTurn
	return nil
}

func (p *Position) Undo() {
	p.pop()
}

func (p *Position) Over() bool {
	return p.over
}

func (p *Position) OnTurn() int {
	return p.onTurn
}

func (p *Position) Spread(player int) int {
	return p.points[player] - p.points[1-player]
}

func (p *Position) RackValue(player int) int {
	return p.rack(player).ScoreOn(p.ld)
}

func (p *Position) RackSize(player int) int {
	return int(p.rack(player).NumTiles())
}

func (p *Position) rack(player int) *tilemapping.Rack {
	return p.racks[player]
}

func (p *Position) place(m *move.Move) {
	p.board.PlayMove(m)
	p.crosses.UpdateForMove(p.board, m)
	p.points[p.onTurn] += m.Score()
	p.scoreless = 0
	p.racks[p.onTurn].Set(m.Leave())
	if p.racks[p.onTurn].NumTiles() == 0 {
		p.over = true
		p.points[p.onTurn] += 2 * p.RackValue(1-p.onTurn)
	}
}

func (p *Position) passTurn() {
	p.scoreless++
	if p.scoreless == maxScoreless {
		p.over = true
		p.points[0] -= p.RackValue(0)
		p.points[1] -= p.RackValue(1)
	}
}
