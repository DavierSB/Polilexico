package endgame

import (
	"github.com/domino14/macondo/board"
	"github.com/domino14/word-golib/tilemapping"
)

func (p *Position) stuckLetters(player int) uint64 {
	playable := playableLetters(p.board)
	var stuck uint64
	for ml, count := range p.rack(player).LetArr {
		if ml != 0 && count > 0 && playable&(1<<uint(ml)) == 0 {
			stuck |= 1 << uint(ml)
		}
	}
	return stuck
}

func (p *Position) spots(player int) map[tilemapping.MachineLetter]squares {
	rack, out := p.rack(player), map[tilemapping.MachineLetter]squares{}
	forEachAnchor(p.board, func(r, c int, fits uint64) {
		for ml, count := range rack.LetArr {
			if ml != 0 && count > 0 && fits&(1<<uint(ml)) != 0 {
				set := out[tilemapping.MachineLetter(ml)]
				set.add(r, c)
				out[tilemapping.MachineLetter(ml)] = set
			}
		}
	})
	return out
}

func playableLetters(b *board.GameBoard) uint64 {
	if b.TilesPlayed() == 0 {
		return uint64(board.TrivialCrossSet)
	}
	var playable uint64
	forEachAnchor(b, func(r, c int, fits uint64) { playable |= fits })
	return playable
}

func forEachAnchor(b *board.GameBoard, visit func(r, c int, fits uint64)) {
	h, v := b.CrossSetsForDir(board.HorizontalDirection), b.CrossSetsForDir(board.VerticalDirection)
	for r := range b.Dim() {
		for c := range b.Dim() {
			if isAnchor(b, r, c) {
				i := b.GetSqIdx(r, c)
				visit(r, c, uint64(h[i])&uint64(v[i]))
			}
		}
	}
}

func isAnchor(b *board.GameBoard, r, c int) bool {
	if occupied(b, r, c) || !inside(b, r, c) {
		return false
	}
	return occupied(b, r-1, c) || occupied(b, r+1, c) || occupied(b, r, c-1) || occupied(b, r, c+1)
}

func occupied(b *board.GameBoard, r, c int) bool {
	return inside(b, r, c) && b.SquaresSlice()[b.GetSqIdx(r, c)] != 0
}

func inside(b *board.GameBoard, r, c int) bool {
	return r >= 0 && c >= 0 && r < b.Dim() && c < b.Dim()
}
