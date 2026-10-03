package duplicate

import (
	"cmp"
	"slices"

	"github.com/domino14/macondo/move"
	"github.com/domino14/macondo/movegen"
	"github.com/domino14/word-golib/tilemapping"

	"lexico/engine/internal/core"
)

type tiebreak func(a, b *move.Move) int

func (d *Game) masterPlays() []*move.Move {
	gen := d.master.MoveGenerator().(*movegen.GordonGenerator)
	plays := copyMoves(gen.GenAll(d.g.RackFor(masterIdx), false))
	d.master.AssignEquity(plays, d.g.Board(), d.g.Bag(), d.g.RackFor(1-masterIdx))
	slices.SortStableFunc(plays, d.compareMasterPlays)
	return plays
}

func (d *Game) compareMasterPlays(a, b *move.Move) int {
	for _, rule := range []tiebreak{byScore, byBlank, byEquity, byTilesPlayed, d.byMainWord, byPosition} {
		if c := rule(a, b); c != 0 {
			return c
		}
	}
	if a.TiebreaksBetter(b) {
		return -1
	}
	return 1
}

func byScore(a, b *move.Move) int {
	return cmp.Compare(b.Score(), a.Score())
}

func byBlank(a, b *move.Move) int {
	return cmp.Compare(rank(usesBlank(a)), rank(usesBlank(b)))
}

func byEquity(a, b *move.Move) int {
	return cmp.Compare(b.Equity(), a.Equity())
}

func byTilesPlayed(a, b *move.Move) int {
	return cmp.Compare(a.TilesPlayed(), b.TilesPlayed())
}

func (d *Game) byMainWord(a, b *move.Move) int {
	return slices.Compare(core.MainWord(d.g.Board(), a), core.MainWord(d.g.Board(), b))
}

func byPosition(a, b *move.Move) int {
	aRow, aCol, aVertical := a.CoordsAndVertical()
	bRow, bCol, bVertical := b.CoordsAndVertical()
	if aVertical != bVertical {
		return cmp.Compare(rank(aVertical), rank(bVertical))
	}
	if aVertical {
		return cmp.Or(cmp.Compare(aRow, bRow), cmp.Compare(aCol, bCol))
	}
	return cmp.Or(cmp.Compare(aCol, bCol), cmp.Compare(aRow, bRow))
}

func usesBlank(m *move.Move) bool {
	return slices.ContainsFunc(m.Tiles(), tilemapping.MachineLetter.IsBlanked)
}

func rank(b bool) int {
	if b {
		return 1
	}
	return 0
}

func copyMoves(moves []*move.Move) []*move.Move {
	out := make([]*move.Move, len(moves))
	for i, m := range moves {
		out[i] = new(move.Move)
		out[i].CopyFrom(m)
	}
	return out
}
