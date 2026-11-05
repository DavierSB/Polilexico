package main

import (
	"errors"
	"log"
	"strings"
	"time"
)

var errDone = errors.New("partidas completas")

type bot struct {
	link     *link
	o        options
	progress *progress
	seated   bool
	played   int
	fallback string
	acted    map[string]bool
	mySeeks  []seek
	asked    map[string]time.Time
	starting map[string]bool
	fresh    bool
}

var challenged = map[string]bool{}

func newBot(l *link, o options, p *progress) *bot {
	return &bot{link: l, o: o, progress: p, played: -1, acted: map[string]bool{}, asked: map[string]time.Time{}, starting: map[string]bool{}, fresh: true}
}

func (b *bot) run() error {
	in := b.reader()
	tick := time.NewTicker(2 * time.Second)
	defer tick.Stop()
	chat := b.chatTicker()
	for {
		select {
		case <-chat:
			b.say()
		case m, ok := <-in:
			if !ok {
				return errors.New("conexión cerrada")
			}
			b.handle(m)
		case <-tick.C:
			b.idle()
		}
		if b.progress.done(b.o) {
			return errDone
		}
	}
}

func (b *bot) reader() <-chan message {
	in := make(chan message, 64)
	go func() {
		defer close(in)
		for {
			m, err := b.link.read()
			if err != nil {
				log.Printf("lectura cortada: %v", err)
				return
			}
			in <- m
		}
	}()
	return in
}

func (b *bot) handle(m message) {
	switch m.T {
	case "welcome":
		log.Printf("conectado")
	case "lobby":
		b.lobby(m)
	case "room":
		b.room(m)
	case "state":
		b.classicState(m)
	case "table":
		b.table(m)
	case "closed":
		b.free("sala cerrada: " + m.Reason)
	case "chat":
		log.Printf("%s dice: %q", m.Name, m.Text)
	case "adjournAsk":
		log.Printf("%s propone aplazar; responde %v", m.Name, b.o.adjourn)
		b.link.send(message{T: "adjournReply", Accept: b.o.adjourn})
	case "rejected":
		b.retry(m.Reason)
	case "refused", "error":
		log.Printf("%s: %s", m.T, m.Reason)
		b.unsay(m.Reason)
	}
}

func (b *bot) retry(reason string) {
	log.Printf("rechazada: %s", reason)
	if b.fallback != "" {
		b.playLater(b.fallback)
		b.fallback = ""
	}
}

func (b *bot) idle() {
	if b.seated {
		return
	}
	for _, mode := range b.modes() {
		if !b.hasSeek(mode) && time.Since(b.asked[mode]) > 10*time.Second {
			log.Printf("publica una %s", mode)
			b.asked[mode] = time.Now()
			b.link.send(b.createMessage(mode))
		}
	}
}

func (b *bot) modes() []string {
	if b.o.create == "" {
		return nil
	}
	return strings.Split(b.o.create, ",")
}

func (b *bot) wants(mode string) bool {
	for _, m := range b.modes() {
		if m == mode {
			return true
		}
	}
	return false
}

func (b *bot) hasSeek(mode string) bool {
	for _, s := range b.mySeeks {
		if s.Mode == mode {
			return true
		}
	}
	return false
}

func (b *bot) createMessage(mode string) message {
	if mode == "duplicate" {
		return message{T: "create", Mode: "duplicate", TurnMs: b.o.turn.Milliseconds(), Rounds: b.o.rounds, Live: true}
	}
	return message{T: "create", Mode: "classic", TimeMs: b.o.clock.Milliseconds()}
}

func (b *bot) lobby(m message) {
	if b.fresh {
		b.fresh = false
		b.cancelLeftovers(mineOnly(m.Seeks))
		return
	}
	b.mySeeks = mineOnly(m.Seeks)
	b.startFull()
	b.challengeFree(m.People)
	if b.seated || !b.o.accept {
		return
	}
	if s, ok := firstOpen(m.Invites, b.o.from, ""); ok {
		b.join(s)
		return
	}
	if s, ok := firstOpen(m.Seeks, b.o.from, b.o.ignore); ok {
		b.join(s)
	}
}

func (b *bot) join(s seek) {
	log.Printf("acepta la %s de %s", s.Mode, s.Host)
	b.seated = true
	b.link.send(message{T: "join", Code: s.ID})
}

func (b *bot) cancelLeftovers(seeks []seek) {
	for _, s := range seeks {
		log.Printf("retira una %s que quedó de antes", s.Mode)
		b.link.send(message{T: "cancel", Code: s.ID})
	}
}

func (b *bot) startFull() {
	for _, s := range b.mySeeks {
		if s.Mode == "duplicate" && b.wants("duplicate") && s.Players >= b.o.players && !b.starting[s.ID] {
			log.Printf("empieza su duplicada con %d jugadores", s.Players)
			b.starting[s.ID] = true
			b.link.send(message{T: "start", Code: s.ID})
		}
	}
}

func mineOnly(seeks []seek) []seek {
	var out []seek
	for _, s := range seeks {
		if s.Mine {
			out = append(out, s)
		}
	}
	return out
}

func firstOpen(seeks []seek, from, ignore string) (seek, bool) {
	for _, s := range seeks {
		if !s.Mine && (from == "" || s.Host == from) && (ignore == "" || !strings.HasPrefix(s.Host, ignore)) {
			return s, true
		}
	}
	return seek{}, false
}

func (b *bot) room(m message) {
	b.seated = true
}

func (b *bot) finish(summary string) {
	if !b.seated {
		return
	}
	b.link.send(message{T: "leave"})
	b.progress.finished++
	b.free("fin de la partida: " + summary)
}

func (b *bot) free(why string) {
	log.Print(why)
	b.seated, b.played, b.progress.mine = false, -1, 0
	b.acted = map[string]bool{}
}

func (b *bot) chatTicker() <-chan time.Time {
	if b.o.chatEvery <= 0 {
		return nil
	}
	return time.NewTicker(b.o.chatEvery).C
}

func (b *bot) say() {
	if !b.seated {
		return
	}
	b.progress.said++
	text := "Hola, este es el " + itoa(b.progress.said) + ".º mensaje."
	log.Printf("escribe: %s", text)
	b.link.send(message{T: "chat", Text: text})
}

func (b *bot) unsay(reason string) {
	if strings.HasPrefix(reason, "Espera") && b.progress.said > 0 {
		b.progress.said--
	}
}

func (b *bot) challengeFree(people []person) {
	if !b.o.challenge || b.seated {
		return
	}
	for _, p := range people {
		key := p.Name + "/" + p.Country
		ignored := b.o.ignore != "" && strings.HasPrefix(p.Name, b.o.ignore)
		if !p.Playing && !challenged[key] && !ignored {
			challenged[key] = true
			log.Printf("reta a %s (%s)", p.Name, p.Country)
			b.link.send(message{T: "challenge", Name: p.Name, Country: p.Country, TimeMs: b.o.clock.Milliseconds()})
		}
	}
}
