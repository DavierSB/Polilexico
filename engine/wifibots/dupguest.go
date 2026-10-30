package main

import (
	"encoding/json"
	"time"
)

type view struct {
	Phase       string
	Board, Rack string
	Round       int
	RemainingMs int64
	Proposal    *struct{ Kind string; Score int }
	IAmReady    bool
	Ready       int
	Waiting     int
	Rounds      []struct {
		Number int
		Plays  []struct{ Name, Kind string; Points, Total int }
	}
}

type dupBot struct {
	name, style, addr string
	readyDelay       time.Duration
	dropRound        int
	acted            map[string]bool
	k                *conn
	dropped          bool
}

func runDupGuest(addr, name, style string, readyDelay time.Duration, dropRound int) {
	b := &dupBot{name: name, style: style, addr: addr, readyDelay: readyDelay, dropRound: dropRound, acted: map[string]bool{}}
	for {
		b.session()
		if !b.reconnectWanted() {
			return
		}
	}
}

func (b *dupBot) reconnectWanted() bool {
	if b.dropped {
		b.dropped = false
		logf(b.name, "vuelvo a conectar en 8 s")
		time.Sleep(8 * time.Second)
		return true
	}
	return false
}

func (b *dupBot) session() {
	k, err := dial(b.addr, b.name)
	if err != nil {
		logf(b.name, "no conecta: %v", err)
		time.Sleep(2 * time.Second)
		b.dropped = true
		return
	}
	b.k = k
	b.acted = map[string]bool{}
	k.send(msg{"t": "hello", "name": b.name, "version": 2})
	k.read(b.handle)
}

func (b *dupBot) handle(m msg) {
	if m["t"] != "table" {
		return
	}
	var v view
	json.Unmarshal([]byte(m["view"].(string)), &v)
	key := v.Phase + string(rune('0'+v.Round))
	if b.acted[key] {
		return
	}
	b.acted[key] = true
	switch {
	case v.Phase == "playing":
		go b.play(v)
	case v.Phase == "results" && !v.IAmReady:
		go b.ready(v)
	case v.Phase == "finished":
		b.report(v)
	}
}

func (b *dupBot) play(v view) {
	logf(b.name, "ronda %d, atril %s, quedan %d ms", v.Round, v.Rack, v.RemainingMs)
	switch b.style {
	case "silent":
		return
	case "pass":
		time.Sleep(2 * time.Second)
		b.k.send(msg{"t": "move", "input": "pasar"})
	case "invalid":
		time.Sleep(2 * time.Second)
		b.k.send(msg{"t": "move", "input": reversedWord(pick(v.Board, v.Rack, 0))})
	case "changer":
		time.Sleep(2 * time.Second)
		b.k.send(msg{"t": "move", "input": pick(v.Board, v.Rack, 3)})
		time.Sleep(4 * time.Second)
		b.k.send(msg{"t": "move", "input": pick(v.Board, v.Rack, 0)})
	default:
		time.Sleep(3 * time.Second)
		b.k.send(msg{"t": "move", "input": pick(v.Board, v.Rack, 0)})
	}
}

func (b *dupBot) ready(v view) {
	b.report(v)
	if b.dropRound > 0 && v.Round == b.dropRound {
		b.dropRound = 0
		b.dropped = true
		logf(b.name, "me desconecto a propósito tras la ronda %d", v.Round)
		b.k.close()
		return
	}
	time.Sleep(b.readyDelay)
	b.k.send(msg{"t": "ready"})
}

func (b *dupBot) report(v view) {
	if len(v.Rounds) == 0 {
		return
	}
	last := v.Rounds[len(v.Rounds)-1]
	for _, p := range last.Plays {
		logf(b.name, "  ronda %d: %-8s %-8s %3d pts  total %4d", last.Number, p.Name, p.Kind, p.Points, p.Total)
	}
}
