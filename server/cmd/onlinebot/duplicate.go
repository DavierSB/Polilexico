package main

import (
	"encoding/json"
	"log"
	"strconv"
	"time"
)

type tableView struct {
	Phase    string
	Board    string
	Rack     string
	Round    int
	IAmReady bool
	Rounds   []struct {
		Number int
		Plays  []struct {
			Name, Kind    string
			Points, Total int
		}
	}
}

func (b *bot) table(m message) {
	var v tableView
	if err := json.Unmarshal([]byte(m.View), &v); err != nil {
		log.Printf("mesa ilegible: %v", err)
		return
	}
	key := v.Phase + strconv.Itoa(v.Round)
	if b.acted[key] {
		return
	}
	b.acted[key] = true
	b.tableStep(v)
}

func (b *bot) tableStep(v tableView) {
	switch {
	case v.Phase == "playing":
		b.mine++
		b.playLater(b.choose(v.Board, v.Rack))
	case v.Phase == "results" && !v.IAmReady:
		report(v)
		go func() { time.Sleep(b.o.delay); b.link.send(message{T: "ready"}) }()
	case v.Phase == "finished":
		report(v)
		b.finish("duplicada de " + strconv.Itoa(len(v.Rounds)) + " rondas")
	}
}

func report(v tableView) {
	if len(v.Rounds) == 0 {
		return
	}
	last := v.Rounds[len(v.Rounds)-1]
	for _, p := range last.Plays {
		log.Printf("  ronda %d: %-10s %-8s %3d pts, total %4d", last.Number, p.Name, p.Kind, p.Points, p.Total)
	}
}
