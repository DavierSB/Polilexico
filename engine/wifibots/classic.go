package main

import (
	"net"
	"time"

	"lexico/engine/modes/classic"
)

type nop struct{}

func (nop) OnChange() {}

func runClassicGuest(addr, name string) {
	for {
		k, err := dial(addr, name)
		if err != nil {
			logf(name, "no conecta: %v", err)
			time.Sleep(2 * time.Second)
			continue
		}
		k.send(msg{"t": "hello", "name": name, "version": 2})
		played := -1
		k.read(func(m msg) {
			if m["t"] == "state" {
				played = guestTurn(k, m["save"].(string), m["paused"].(bool), played)
			}
		})
		return
	}
}

func guestTurn(k *conn, save string, paused bool, played int) int {
	mirror, err := classic.LoadMirror(save, true, nop{})
	if err != nil {
		logf(k.who, "espejo: %v", err)
		return played
	}
	defer mirror.Close()
	g := mirror.Game()
	s := g.Status()
	logf(k.who, "estado: %d jugadas, marcador %d-%d, turno de %s, pausa %v, fin %v", g.MoveCount(), s.HumanScore, s.BotScore, turnOf(s.HumanToMove), paused, s.Over)
	if s.Over || paused || s.HumanToMove || g.MoveCount() == played {
		return played
	}
	in := pick(g.Board(), g.OpponentRack(), 0)
	go func() { time.Sleep(3 * time.Second); k.send(msg{"t": "move", "input": in}) }()
	return g.MoveCount()
}

func turnOf(human bool) string {
	if human {
		return "A (anfitrión)"
	}
	return "B (invitado)"
}

func runClassicHost(addr, _ string, turnMs int) {
	ln, err := net.Listen("tcp", addr)
	if err != nil {
		panic(err)
	}
	logf("anfitrión", "escuchando en %s", addr)
	c, _ := ln.Accept()
	k := wrap(c, "teléfono")
	var m *classic.Match
	push := func() {
		if m == nil {
			return
		}
		s, _ := m.Save()
		k.send(msg{"t": "state", "save": s, "paused": m.Paused()})
		hostTurn(m)
	}
	k.read(func(in msg) {
		switch in["t"] {
		case "hello":
			k.send(msg{"t": "welcome", "host": "PC"})
			m, err = classic.NewLocalMatch("PC", in["name"].(string), classic.StarterHuman, int64(turnMs), 60000, true, notifier{push})
			if err != nil {
				panic(err)
			}
			push()
		case "move":
			if m.Game().Status().HumanToMove {
				k.send(msg{"t": "rejected", "reason": "No es tu turno"})
			} else if _, err := m.Play(in["input"].(string)); err != nil {
				k.send(msg{"t": "rejected", "reason": err.Error()})
			}
		case "pause":
			m.Pause()
		case "resume":
			m.Resume()
		}
	})
}

var hostPlayed = -1

func hostTurn(m *classic.Match) {
	g := m.Game()
	s := g.Status()
	if s.Over || m.Paused() || !s.HumanToMove || g.MoveCount() == hostPlayed {
		return
	}
	hostPlayed = g.MoveCount()
	in := pick(g.Board(), g.Rack(), 0)
	go func() {
		time.Sleep(3 * time.Second)
		if _, err := m.Play(in); err != nil {
			logf("PC", "jugada: %v", err)
		} else {
			logf("PC", "juega %s", in)
		}
	}()
}
