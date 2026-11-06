package main

import (
	"log"
	"math/rand/v2"
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
	b.proposeIfDue(g.MoveCount()/2 + 1)
	if s.HumanToMove != first || g.MoveCount() == b.played {
		return
	}
	b.played = g.MoveCount()
	b.progress.mine++
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
	if b.o.style == "random" {
		return pickRandom(board, rack)
	}
	if b.o.style != "mixed" {
		return best
	}
	switch b.progress.mine % 5 {
	case 2:
		return reversedWord(best)
	case 4:
		return "pasar"
	}
	return best
}

func (b *bot) playLater(in string) {
	drop := b.dropsAfterThis()
	wait := b.wait()
	go func() {
		time.Sleep(wait)
		log.Printf("juega %s (pensó %s)", in, wait.Round(time.Second))
		b.link.send(message{T: "move", Input: in})
		if drop {
			b.dropNow()
		}
	}()
}

func (b *bot) dropsAfterThis() bool {
	if b.o.dropEvery > 0 {
		return b.progress.mine%b.o.dropEvery == 0
	}
	drop := b.o.drop > 0 && b.progress.mine == b.o.drop && !b.progress.dropped
	if drop {
		b.progress.dropped = true
	}
	return drop
}

func (b *bot) wait() time.Duration {
	if b.o.delayMax <= b.o.delay {
		return b.o.delay
	}
	return b.o.delay + rand.N(b.o.delayMax-b.o.delay)
}

func (b *bot) dropNow() {
	time.Sleep(time.Second)
	b.progress.away = b.o.dropFor
	log.Printf("se desconecta a propósito por %s", b.o.dropFor)
	b.link.conn.CloseNow()
}
