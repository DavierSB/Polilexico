package main

import (
	"encoding/json"
	"net"
	"strings"
	"sync"
	"time"

	"lexico/engine/modes/duplicate"
)

type dupHost struct {
	mu      sync.Mutex
	table   *duplicate.Table
	phone   *conn
	name    string
	bots    []string
	styles  []string
	readies []string
	acted   map[string]bool
}

type notifier struct{ f func() }

func (n notifier) OnChange() { go n.f() }

func runDupHost(addr string, bots, styles, readies []string, turnMs, rounds int, live bool) {
	ln, err := net.Listen("tcp", addr)
	if err != nil {
		panic(err)
	}
	h := &dupHost{bots: bots, styles: styles, readies: readies, acted: map[string]bool{}}
	logf("anfitrión", "escuchando en %s; jugadores simulados: %v", addr, bots)
	for {
		c, err := ln.Accept()
		if err != nil {
			return
		}
		k := wrap(c, "teléfono")
		go k.read(func(m msg) { h.handle(k, m, turnMs, rounds, live) })
	}
}

func (h *dupHost) handle(k *conn, m msg, turnMs, rounds int, live bool) {
	switch m["t"] {
	case "hello":
		h.admit(k, m["name"].(string), turnMs, rounds, live)
	case "move":
		if _, err := h.table.Propose(h.name, m["input"].(string)); err != nil {
			k.send(msg{"t": "rejected", "reason": err.Error()})
		}
	case "ready":
		h.table.Ready(h.name)
	case "bye":
		h.table.SetPresent(h.name, false)
	}
}

func (h *dupHost) admit(k *conn, name string, turnMs, rounds int, live bool) {
	h.mu.Lock()
	h.phone, h.name = k, name
	h.mu.Unlock()
	k.send(msg{"t": "welcome", "host": "PC"})
	if h.table != nil {
		h.table.SetPresent(name, true)
		h.push()
		return
	}
	players := append([]string{"PC"}, h.bots...)
	players = append(players, name)
	k.send(msg{"t": "lobby", "players": players})
	time.Sleep(4 * time.Second)
	t, err := duplicate.NewTable(strings.Join(players, "\n"), int64(turnMs), rounds, live, notifier{h.changed})
	if err != nil {
		panic(err)
	}
	h.table = t
	h.changed()
}

func (h *dupHost) changed() {
	h.push()
	h.botsAct()
}

func (h *dupHost) push() {
	h.mu.Lock()
	defer h.mu.Unlock()
	if h.phone == nil || h.table == nil {
		return
	}
	v, _ := h.table.View(h.name)
	h.phone.send(msg{"t": "table", "view": v})
}

func (h *dupHost) botsAct() {
	for i, b := range append([]string{"PC"}, h.bots...) {
		text, _ := h.table.View(b)
		var v view
		json.Unmarshal([]byte(text), &v)
		key := b + v.Phase + string(rune('0'+v.Round))
		h.mu.Lock()
		seen := h.acted[key]
		h.acted[key] = true
		h.mu.Unlock()
		if !seen {
			go h.botAct(b, at(append([]string{"best"}, h.styles...), i), at(append([]string{"1000"}, h.readies...), i), v)
		}
	}
}

func (h *dupHost) botAct(b, style, ready string, v view) {
	switch v.Phase {
	case "playing":
		if style == "silent" {
			return
		}
		time.Sleep(2 * time.Second)
		in := pick(v.Board, v.Rack, 0)
		if style == "pass" {
			in = "pasar"
		}
		if _, err := h.table.Propose(b, in); err != nil {
			logf(b, "jugada rechazada: %v", err)
		} else {
			logf(b, "juega %s", in)
		}
	case "results":
		time.Sleep(ms(ready))
		h.table.Ready(b)
		logf(b, "listo para la ronda siguiente")
	case "finished":
		logf(b, "partida terminada")
	}
}
