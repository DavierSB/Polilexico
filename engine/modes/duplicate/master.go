package duplicate

import (
	"github.com/domino14/macondo/move"

	"lexico/engine/internal/core"
)

func (d *Game) masterPlays() []*move.Move {
	return core.MasterPlays(d.master, d.g, masterIdx)
}

func (d *Game) compareMasterPlays(a, b *move.Move) int {
	return core.MasterOrder(d.g.Board())(a, b)
}
