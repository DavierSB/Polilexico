package engine

import (
	"errors"
	"fmt"
	"strings"

	"github.com/domino14/macondo/game"

	"lexico/engine/internal/core"
)

var errEmptyRack = errors.New("el atril está vacío")

func gameFromPosition(board, rack string) (*game.Game, error) {
	rows, err := core.ParseBoard(board)
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
