package sprint

import (
	"errors"
	"sort"

	"github.com/domino14/macondo/game"
	"github.com/domino14/macondo/move"

	"lexico/engine/internal/core"
)

const bingoTiles = 7

var errNotBingo = errors.New("tiene que ser un scrabble: coloca las 7 fichas del atril")

type Solution struct {
	Coords string
	Tiles  string
	Score  int
}

type puzzle struct {
	g         *game.Game
	board     string
	rack      string
	solutions []Solution
}

func newPuzzle(g *game.Game, bingos []*move.Move) *puzzle {
	return &puzzle{g: g.Copy(), board: core.BoardText(g), rack: core.RackText(g, g.PlayerOnTurn()),
		solutions: solutionsOf(bingos)}
}

func (p *puzzle) check(input string) (Solution, error) {
	m, err := core.ParseInput(p.g, p.g.PlayerOnTurn(), input)
	if err != nil {
		return Solution{}, err
	}
	if !isBingo(m) {
		return Solution{}, errNotBingo
	}
	return solutionOf(m), nil
}

func isBingo(m *move.Move) bool {
	return m.Action() == move.MoveTypePlay && m.TilesPlayed() == bingoTiles
}

func solutionsOf(bingos []*move.Move) []Solution {
	out := make([]Solution, len(bingos))
	for i, m := range bingos {
		out[i] = solutionOf(m)
	}
	sort.SliceStable(out, func(i, j int) bool { return out[i].Score > out[j].Score })
	return out
}

func solutionOf(m *move.Move) Solution {
	p := core.PlayOf(m)
	return Solution{Coords: p.Coords, Tiles: p.Tiles, Score: p.Score}
}
