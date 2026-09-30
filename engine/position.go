package engine

import (
	"errors"
	"fmt"
	"strings"

	"github.com/domino14/macondo/game"
	"github.com/domino14/word-golib/tilemapping"

	"lexico/engine/internal/core"
)

const boardSize = 15

var errEmptyRack = errors.New("el atril está vacío")

func gameFromPosition(board, rack string) (*game.Game, error) {
	rows, err := parseBoard(board)
	if err != nil {
		return nil, err
	}
	g, err := core.NewGameFromBoard(rows, rack)
	if err != nil {
		return nil, fmt.Errorf("posición imposible: %w", err)
	}
	if g.RackFor(0).NumTiles() == 0 {
		return nil, errEmptyRack
	}
	return g, nil
}

func placedRack(placement string) string {
	fields := strings.Fields(placement)
	if len(fields) < 2 {
		return ""
	}
	var rack strings.Builder
	for _, tile := range tilesOf(core.ExpandEnhe(fields[1])) {
		rack.WriteString(asRackTile(tile))
	}
	return rack.String()
}

func tilesOf(word string) []string {
	var tiles []string
	for rest := []rune(word); len(rest) > 0; {
		n := tileLength(rest)
		tiles = append(tiles, string(rest[:n]))
		rest = rest[n:]
	}
	return tiles
}

func tileLength(word []rune) int {
	if word[0] == '[' {
		for i, r := range word {
			if r == ']' {
				return i + 1
			}
		}
	}
	return 1
}

func asRackTile(tile string) string {
	switch {
	case tile == ".":
		return ""
	case tile != strings.ToUpper(tile):
		return "?"
	}
	return tile
}

func parseBoard(board string) ([][]tilemapping.MachineLetter, error) {
	squares := strings.Fields(board)
	if len(squares) != boardSize*boardSize {
		return nil, fmt.Errorf("el tablero tiene %d casillas, no %d", len(squares), boardSize*boardSize)
	}
	alphabet, err := spanishAlphabet()
	if err != nil {
		return nil, err
	}
	return parseSquares(squares, alphabet)
}

func parseSquares(squares []string, alphabet *tilemapping.TileMapping) ([][]tilemapping.MachineLetter, error) {
	rows := emptyRows()
	for i, sq := range squares {
		ml, err := parseSquare(sq, alphabet)
		if err != nil {
			return nil, fmt.Errorf("casilla %s: %w", core.FormatCoords(i/boardSize, i%boardSize, false), err)
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

func spanishAlphabet() (*tilemapping.TileMapping, error) {
	g, err := core.NewGame("a", "b")
	if err != nil {
		return nil, err
	}
	return g.Alphabet(), nil
}
