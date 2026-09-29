package review

import (
	"strings"

	"lexico/engine/internal/core"
)

const size = 15

// grid es el tablero que se va armando con las jugadas del registro, para mostrar el de antes
// de cada turno. Cada casilla, como en engine.BestMoves: "" vacia, "CH", minuscula = comodin.
type grid [size][size]string

func newGrid() *grid {
	return &grid{}
}

// play pone una colocacion ("H8 CA.A (12 pts)"); pases, cambios y jugadas perdidas no cambian nada.
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

// text: las 225 casillas separadas por espacios, "." las vacias.
func (b *grid) text() string {
	squares := make([]string, 0, size*size)
	for _, row := range b {
		for _, s := range row {
			squares = append(squares, orDot(s))
		}
	}
	return strings.Join(squares, " ")
}

// put pone la ficha i de la palabra; "." (letra que ya estaba) no cambia nada.
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

// tokens parte una palabra de macondo en fichas: "[CH]" es una, "." tambien.
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

// plain quita los corchetes de los digrafos.
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
