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

func (b *bot) propose(when string) {
	log.Printf("propone aplazar (%s)", when)
	b.link.send(message{T: "adjourn"})
}
