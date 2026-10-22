package preendgame

import (
	"github.com/domino14/macondo/board"
	"github.com/domino14/macondo/cross_set"
	"github.com/domino14/macondo/move"
	"github.com/domino14/macondo/movegen"
	"github.com/domino14/word-golib/tilemapping"

	"lexico/engine/internal/core"
)

type spotFinder struct {
	board   *board.GameBoard
	gen     *movegen.GordonGenerator
	crosses cross_set.Generator
	rack    *tilemapping.Rack
	q       tilemapping.MachineLetter
}

func newSpotFinder(b *board.GameBoard, q tilemapping.MachineLetter) (*spotFinder, error) {
	f := &spotFinder{board: b.Copy(), q: q}
	var err error
	if f.gen, err = core.NewGenerator(f.board); err != nil {
		return nil, err
	}
	if f.crosses, err = core.CrossSets(); err != nil {
		return nil, err
	}
	ld, err := core.Distribution()
	if err != nil {
		return nil, err
	}
	f.rack = tilemapping.NewRack(ld.TileMapping())
	return f, nil
}

func (f *spotFinder) canPlace(b *board.GameBoard, m *move.Move, tiles tilemapping.MachineWord) bool {
	f.board.CopyFrom(b)
	if m.Action() == move.MoveTypePlay {
		f.board.PlayMove(m)
		f.crosses.UpdateForMove(f.board, m)
	}
	f.rack.Set(tiles)
	f.gen.GenAll(f.rack, false)
	return twoApart(f.footprints(f.gen.Plays()))
}

func (f *spotFinder) footprints(plays []*move.Move) []footprint {
	seen := map[footprint]bool{}
	var out []footprint
	for _, m := range plays {
		if m.Action() != move.MoveTypePlay || count(m.Tiles(), f.q) == 0 {
			continue
		}
		if fp := footprintOf(m, f.board.Dim()); !seen[fp] {
			seen[fp] = true
			out = append(out, fp)
		}
	}
	return out
}
