package core

import (
	"github.com/domino14/macondo/board"
	"github.com/domino14/macondo/move"
	"github.com/domino14/word-golib/tilemapping"
)

func MainWord(b *board.GameBoard, m *move.Move) []tilemapping.MachineLetter {
	row, col, vertical := m.CoordsAndVertical()
	word := make([]tilemapping.MachineLetter, len(m.Tiles()))
	for i, t := range m.Tiles() {
		if t == 0 {
			t = b.GetLetter(row, col)
		}
		word[i] = t.Unblank()
		row, col = next(row, col, vertical)
	}
	return word
}

func next(row, col int, vertical bool) (int, int) {
	if vertical {
		return row + 1, col
	}
	return row, col + 1
}
