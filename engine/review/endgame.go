package review

import (
	"errors"
	"strings"

	"github.com/domino14/macondo/game"
	macondo "github.com/domino14/macondo/move"

	"lexico/engine/internal/core"
	"lexico/engine/internal/endgame"
)

const (
	endgameLines = 15
	fullRack     = 7
)

var errNoEndgame = errors.New("en este turno aún quedan fichas en la bolsa")

type position struct {
	rack   string
	scores [2]int
}

type Endgame struct {
	Depth int
	Exact bool
	lines []*EndgameLine
}

type EndgameLine struct {
	Description string
	Score       int
	Spread      int
	MoverLeft   string
	OtherLeft   string
	MoverDelta  int
	OtherDelta  int
	Depth       int
	steps       []*Move
}

func (e *Endgame) LineCount() int {
	return len(e.lines)
}

func (e *Endgame) LineAt(i int) *EndgameLine {
	if i < 0 || i >= len(e.lines) {
		return nil
	}
	return e.lines[i]
}

func (l *EndgameLine) StepCount() int {
	return len(l.steps)
}

func (l *EndgameLine) StepAt(i int) *Move {
	if i < 0 || i >= len(l.steps) {
		return nil
	}
	return l.steps[i]
}

func positionOf(rack string, scores *tally, mine bool) position {
	if mine {
		return position{rack: rack, scores: [2]int{scores.mine, scores.theirs}}
	}
	return position{rack: rack, scores: [2]int{scores.theirs, scores.mine}}
}

func (t *Turn) bagEmpty() bool {
	return core.TileTotal() > 0 && t.unseen() <= fullRack
}

func (t *Turn) bag() int {
	if core.TileTotal() == 0 {
		return 0
	}
	return max(t.unseen()-fullRack, 0)
}

func (t *Turn) unseen() int {
	return core.TileTotal() - placedTiles(t.Board) - len(strings.Fields(t.Rack))
}

func (t *Turn) endgameGame() (*game.Game, error) {
	rows, err := core.ParseBoard(t.Board)
	if err != nil {
		return nil, err
	}
	return core.NewEndgame(rows, t.position.rack, t.position.scores)
}

func (t *Turn) isPlayed(m *macondo.Move) bool {
	return len(t.marks) > 0 && describe(m) == t.marks[0].Description
}

func endgameOf(g *game.Game, a endgame.Analysis) *Endgame {
	e := &Endgame{Depth: a.Depth, Exact: a.Exact}
	for _, l := range a.Lines {
		e.lines = append(e.lines, lineOf(g, l))
	}
	return e
}

func lineOf(g *game.Game, l endgame.Line) *EndgameLine {
	out := &EndgameLine{Description: describe(l.Move), Score: l.Move.Score(), Spread: l.Spread, Depth: l.Depth}
	for _, m := range l.Reply {
		out.steps = append(out.steps, &Move{Description: describe(m), Score: m.Score()})
	}
	out.setEnding(played(g, l))
	return out
}

func played(g *game.Game, l endgame.Line) *game.Game {
	end := g.Copy()
	for _, m := range append([]*macondo.Move{l.Move}, l.Reply...) {
		if end.PlayMove(m, false, 0) != nil {
			break
		}
	}
	return end
}

func (l *EndgameLine) setEnding(end *game.Game) {
	mover, other := core.RackValue(end, 0), core.RackValue(end, 1)
	l.MoverLeft, l.OtherLeft = core.RackText(end, 0), core.RackText(end, 1)
	switch {
	case l.MoverLeft == "":
		l.MoverDelta, l.OtherDelta = other, -other
	case l.OtherLeft == "":
		l.MoverDelta, l.OtherDelta = -mover, mover
	default:
		l.MoverDelta, l.OtherDelta = -mover, -other
	}
}

func describe(m *macondo.Move) string {
	return core.Describe(core.PlayOf(m))
}

func placedTiles(board string) int {
	placed := 0
	for _, sq := range strings.Fields(board) {
		if sq != "." {
			placed++
		}
	}
	return placed
}
