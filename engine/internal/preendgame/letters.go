package preendgame

import (
	"github.com/domino14/macondo/move"
	"github.com/domino14/word-golib/tilemapping"

	"lexico/engine/internal/core"
)

const blank tilemapping.MachineLetter = 0

type letters struct {
	q, u, e, i, a, d tilemapping.MachineLetter
}

func newLetters() (letters, error) {
	ld, err := core.Distribution()
	if err != nil {
		return letters{}, err
	}
	var ls letters
	for name, target := range map[string]*tilemapping.MachineLetter{"Q": &ls.q, "U": &ls.u, "E": &ls.e,
		"I": &ls.i, "A": &ls.a, "D": &ls.d} {
		if *target, err = ld.TileMapping().Val(name); err != nil {
			return ls, err
		}
	}
	return ls, nil
}

func (l letters) whereIsQ(rack, unseenTiles *tilemapping.Rack) place {
	switch {
	case rack.CountOf(l.q) > 0:
		return mine
	case unseenTiles.CountOf(l.q) > 0:
		return unseen
	}
	return onBoard
}

func (l letters) singles(r *tilemapping.Rack) []tilemapping.MachineLetter {
	var out []tilemapping.MachineLetter
	for ml, n := range r.LetArr {
		if n > 0 && tilemapping.MachineLetter(ml) != l.q {
			out = append(out, tilemapping.MachineLetter(ml))
		}
	}
	return out
}

func (l letters) partnered(r *tilemapping.Rack) bool {
	blanks := r.CountOf(blank)
	if r.CountOf(l.u) == 0 {
		if blanks == 0 {
			return false
		}
		blanks--
	}
	if r.CountOf(l.e)+r.CountOf(l.i) > 0 || blanks > 0 {
		return true
	}
	return r.CountOf(l.a) > 0 && r.CountOf(l.d) > 0
}

func (l letters) plays(m *move.Move) bool {
	return m.Action() == move.MoveTypePlay && count(m.Tiles(), l.q) > 0
}

func (l letters) exchanges(m *move.Move) bool {
	return m.Action() == move.MoveTypeExchange && count(m.Tiles(), l.q) > 0
}

func count(word tilemapping.MachineWord, letter tilemapping.MachineLetter) int {
	n := 0
	for _, t := range word {
		if t == letter {
			n++
		}
	}
	return n
}
