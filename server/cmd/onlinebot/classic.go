package main

import (
	"log"
	"time"

	"lexico/engine/modes/classic"
)

type nop struct{}

func (nop) OnChange() {}

func (b *bot) classicState(m message) {
	mirror, err := classic.LoadMirror(m.Save, true, nop{})
	if err != nil {
		log.Printf("partida ilegible: %v", err)
		return
	}
	defer mirror.Close()
	b.react(mirror.Game(), m.First)
}

func (b *bot) react(g *classic.Game, first bool) {
	s := g.Status()
	if s.Over {
		b.finish(score(s, first))
		return
	}
	if s.HumanToMove != first || g.MoveCount() == b.played {
		return
	}
	b.played = g.MoveCount()
	b.mine++
	b.playLater(b.choose(g.Board(), rackOf(g, first)))
}

func rackOf(g *classic.Game, first bool) string {
	if first {
		return g.Rack()
	}
	return g.OpponentRack()
}

func score(s *classic.Status, first bool) string {
	mine, theirs := s.BotScore, s.HumanScore
	if first {
		mine, theirs = theirs, mine
	}
	return itoa(mine) + " a " + itoa(theirs)
}

func (b *bot) choose(board, rack string) string {
	best := pick(board, rack)
	b.fallback = best
	if b.o.style != "mixed" {
		return best
	}
	switch b.mine % 5 {
	case 2:
		return reversedWord(best)
	case 4:
		return "pasar"
	}
	return best
}

func (b *bot) playLater(in string) {
	drop := b.o.drop > 0 && b.mine == b.o.drop && !b.progress.dropped
	if drop {
		b.progress.dropped = true
	}
	go func() {
		time.Sleep(b.o.delay)
		log.Printf("juega %s", in)
		b.link.send(message{T: "move", Input: in})
		if drop {
			b.dropNow()
		}
	}()
}

func (b *bot) dropNow() {
	time.Sleep(time.Second)
	log.Printf("se desconecta a propósito")
	b.link.conn.CloseNow()
}
