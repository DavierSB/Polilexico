package review

import (
	"strings"

	"lexico/engine/internal/core"
)

const size = 15

type grid [size][size]string

func newGrid() *grid {
	return &grid{}
}

func (b *grid) play(description string) {
	fields := strings.Fields(description)
	if len(fields) < 2 {
		return
	}
	row, col, vertical, err := core.ParseCoords(fields[0])
	if err != nil {
		return
	}
	for i, tile := range plain(tokens(fields[1])) {
		b.put(row, col, vertical, i, tile)
	}
}

func (b *grid) text() string {
	squares := make([]string, 0, size*size)
	for _, row := range b {
		for _, s := range row {
			squares = append(squares, orDot(s))
		}
	}
	return strings.Join(squares, " ")
}

func (b *grid) put(row, col int, vertical bool, i int, tile string) {
	if vertical {
		row += i
	} else {
		col += i
	}
	if tile != "." && row < size && col < size {
		b[row][col] = tile
	}
}

func tokens(word string) []string {
	var out []string
	for rest := word; rest != ""; {
		n := tokenLen(rest)
		out = append(out, rest[:n])
		rest = rest[n:]
	}
	return out
}

func tokenLen(word string) int {
	if word[0] == '[' {
		if end := strings.IndexByte(word, ']'); end > 0 {
			return end + 1
		}
	}
	return len(string([]rune(word)[0]))
}

func plain(tiles []string) []string {
	out := make([]string, len(tiles))
	for i, t := range tiles {
		out[i] = strings.Trim(t, "[]")
	}
	return out
}

func orDot(square string) string {
	if square == "" {
		return "."
	}
	return square
}
