package endgame

import "github.com/domino14/macondo/move"

const boardSide = 15

type squares [4]uint64

func squaresOf(m *move.Move) squares {
	row, col, vertical := m.CoordsAndVertical()
	var set squares
	for i, t := range m.Tiles() {
		if t != 0 {
			set.add(row+boolInt(vertical)*i, col+boolInt(!vertical)*i)
		}
	}
	return set
}

func (s *squares) add(r, c int) {
	i := r*boardSide + c
	s[i/64] |= 1 << uint(i%64)
}

func (s squares) overlaps(o squares) bool {
	return s[0]&o[0] != 0 || s[1]&o[1] != 0 || s[2]&o[2] != 0 || s[3]&o[3] != 0
}

func (s squares) within(o squares) bool {
	return s[0]&^o[0] == 0 && s[1]&^o[1] == 0 && s[2]&^o[2] == 0 && s[3]&^o[3] == 0
}

func boolInt(b bool) int {
	if b {
		return 1
	}
	return 0
}
