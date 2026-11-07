package main

import (
	"log"
	"time"
)

func (b *bot) proposeIfDue(round int) {
	if b.o.proposeAt == 0 || round != b.o.proposeAt || b.progress.proposed {
		return
	}
	b.progress.proposed = true
	b.propose("al empezar")
	time.AfterFunc(b.o.again, func() { b.propose("otra vez") })
}

func (b *bot) rivalPresence(online bool) {
	gen := b.awayGen.Add(1)
	if online || b.o.awayAfter == 0 {
		return
	}
	log.Printf("el rival se desconectó; aplaza en %s si no vuelve", b.o.awayAfter)
	time.AfterFunc(b.o.awayAfter, func() {
		if b.awayGen.Load() == gen {
			b.propose("rival fuera")
		}
	})
}

func (b *bot) proposeIfResumed(moves int) {
	if b.resumeOk || b.o.resumedFor == 0 {
		return
	}
	b.resumeOk = true
	if moves == 0 {
		return
	}
	log.Printf("partida retomada; propone aplazar en %s", b.o.resumedFor)
	gen := b.seatGen.Load()
	time.AfterFunc(b.o.resumedFor, func() {
		if b.seatGen.Load() == gen {
			b.propose("retomada")
		}
	})
}

func (b *bot) propose(when string) {
	log.Printf("propone aplazar (%s)", when)
	b.link.send(message{T: "adjourn"})
}
