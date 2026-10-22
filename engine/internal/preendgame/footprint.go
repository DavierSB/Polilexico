package preendgame

import "github.com/domino14/macondo/move"

type bits [4]uint64

type footprint struct {
	cells bits
	halo  bits
}

func twoApart(fs []footprint) bool {
	for i := range fs {
		for j := i + 1; j < len(fs); j++ {
			if fs[i].apart(fs[j]) {
				return true
			}
		}
	}
	return false
}

func (a footprint) apart(b footprint) bool {
	for k := range a.halo {
		if a.halo[k]&b.cells[k] != 0 {
			return false
		}
	}
	return true
}

func footprintOf(m *move.Move, dim int) footprint {
	var fp footprint
	row, col, vertical := m.CoordsAndVertical()
	for i := range m.Tiles() {
		r, c := row, col+i
		if vertical {
			r, c = row+i, col
		}
		fp.cells.set(r, c, dim)
		fp.halo.surround(r, c, dim)
	}
	return fp
}

func (b *bits) surround(r, c, dim int) {
	for _, d := range [5][2]int{{0, 0}, {-1, 0}, {1, 0}, {0, -1}, {0, 1}} {
		b.set(r+d[0], c+d[1], dim)
	}
}

func (b *bits) set(r, c, dim int) {
	if r < 0 || c < 0 || r >= dim || c >= dim {
		return
	}
	i := r*dim + c
	b[i/64] |= 1 << uint(i%64)
}
