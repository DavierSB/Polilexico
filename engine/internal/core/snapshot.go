package core

import (
	"errors"
	"fmt"
	"strings"

	"github.com/domino14/macondo/game"
	"github.com/domino14/word-golib/tilemapping"
)

const boardSize = 15

const fullRack = 7

var ErrNotEndgame = errors.New("aún quedan fichas en la bolsa")

func ParseBoard(board string) ([][]tilemapping.MachineLetter, error) {
	squares := strings.Fields(board)
	if len(squares) != boardSize*boardSize {
		return nil, fmt.Errorf("el tablero tiene %d casillas, no %d", len(squares), boardSize*boardSize)
	}
	if err := Ready(); err != nil {
		return nil, err
	}
	return parseSquares(squares, rules.LetterDistribution().TileMapping())
}

func NewEndgame(rows [][]tilemapping.MachineLetter, rack string, scores [2]int) (*game.Game, error) {
	g, err := NewGameFromBoard(rows, rack)
	if err != nil {
		return nil, err
	}
	if g.Bag().TilesRemaining() > fullRack {
		return nil, ErrNotEndgame
	}
	g.SetPointsFor(0, scores[0])
	g.SetPointsFor(1, scores[1])
	return g, g.SetRackForOnly(1, bagRack(g))
}

func TileTotal() int {
	if Ready() != nil {
		return 0
	}
	return int(rules.LetterDistribution().NumTotalLetters())
}

func bagRack(g *game.Game) *tilemapping.Rack {
	rack := tilemapping.NewRack(g.Alphabet())
	rack.Set(g.Bag().Peek())
	return rack
}

func parseSquares(squares []string, alphabet *tilemapping.TileMapping) ([][]tilemapping.MachineLetter, error) {
	rows := emptyRows()
	for i, sq := range squares {
		ml, err := parseSquare(sq, alphabet)
		if err != nil {
			return nil, fmt.Errorf("casilla %s: %w", FormatCoords(i/boardSize, i%boardSize, false), err)
		}
		rows[i/boardSize][i%boardSize] = ml
	}
	return rows, nil
}

func parseSquare(sq string, alphabet *tilemapping.TileMapping) (tilemapping.MachineLetter, error) {
	if sq == "." {
		return 0, nil
	}
	if len([]rune(sq)) > 1 {
		sq = "[" + sq + "]"
	}
	return alphabet.Val(sq)
}

func emptyRows() [][]tilemapping.MachineLetter {
	rows := make([][]tilemapping.MachineLetter, boardSize)
	for i := range rows {
		rows[i] = make([]tilemapping.MachineLetter, boardSize)
	}
	return rows
}
