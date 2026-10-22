package preendgame

import (
	"slices"

	"github.com/domino14/word-golib/tilemapping"

	"lexico/engine/internal/core"
	"lexico/engine/internal/endgame"
)

const maxDeals = 300

type deal struct {
	mine   *tilemapping.Rack
	theirs *tilemapping.Rack
}

func (d deal) apply(pos *endgame.Position, me int) {
	pos.SetRack(me, d.mine)
	pos.SetRack(1-me, d.theirs)
}

func (b *preBot) deals(unseenTiles *tilemapping.Rack, drawn int) ([]deal, error) {
	rest := unseenTiles.Copy()
	rest.Take(b.letters.q)
	var picks []tilemapping.MachineWord
	pick(slices.Clone(rest.LetArr), 0, drawn-1, nil, &picks)
	var out []deal
	for _, p := range thin(picks, maxDeals) {
		d, err := b.dealOf(rest, p)
		if err != nil {
			return nil, err
		}
		out = append(out, d)
	}
	return out, nil
}

func pick(counts []int, from, left int, current tilemapping.MachineWord, out *[]tilemapping.MachineWord) {
	if left == 0 {
		*out = append(*out, slices.Clone(current))
		return
	}
	for ml := from; ml < len(counts); ml++ {
		if counts[ml] == 0 {
			continue
		}
		counts[ml]--
		pick(counts, ml, left-1, append(current, tilemapping.MachineLetter(ml)), out)
		counts[ml]++
	}
}

func thin(picks []tilemapping.MachineWord, limit int) []tilemapping.MachineWord {
	if len(picks) <= limit {
		return picks
	}
	out := make([]tilemapping.MachineWord, 0, limit)
	for i := range limit {
		out = append(out, picks[i*len(picks)/limit])
	}
	return out
}

func (b *preBot) dealOf(rest *tilemapping.Rack, chosen tilemapping.MachineWord) (deal, error) {
	ld, err := core.Distribution()
	if err != nil {
		return deal{}, err
	}
	mine := tilemapping.NewRack(ld.TileMapping())
	mine.Set(append(slices.Clone(chosen), b.letters.q))
	theirs := rest.Copy()
	for _, t := range chosen {
		theirs.Take(t)
	}
	return deal{mine: mine, theirs: theirs}, nil
}
