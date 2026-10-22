package preendgame

import (
	"github.com/domino14/macondo/board"
	"github.com/domino14/word-golib/tilemapping"
)

type place int

const (
	onBoard place = iota
	mine
	unseen
)

type situation struct {
	where    place
	bag      int
	drawBase int
	unseen   *tilemapping.Rack
	board    *board.GameBoard
}

func (b *preBot) situation(drawBase int) situation {
	g := b.game
	me := g.PlayerOnTurn()
	s := situation{bag: g.Bag().TilesRemaining(), drawBase: drawBase, unseen: b.unseenRack(1 - me), board: g.Board()}
	s.where = b.letters.whereIsQ(g.RackFor(me), s.unseen)
	return s
}

func (b *preBot) unseenRack(opponent int) *tilemapping.Rack {
	r := tilemapping.NewRack(b.game.Alphabet())
	r.Set(append(b.game.Bag().Peek(), b.game.RackFor(opponent).TilesOn()...))
	return r
}
