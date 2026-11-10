//go:build js && wasm

package main

import "lexico/engine/modes/classic"

type classicState struct {
	Opponent     string
	Host         string
	Local        bool
	Study        bool
	Board        string
	Rack         string
	OpponentRack string
	Unseen       string
	Status       *classic.Status
	Moves        []*classic.Move
	Clocks       *classic.Clocks
	Paused       bool
	Result       *classic.Result
	BotError     string
}

func read(m *classic.Match) classicState {
	g := m.Game()
	return classicState{
		Opponent: g.Opponent(), Host: g.Host(), Local: g.Local(), Study: m.Study(),
		Board: g.Board(), Rack: g.Rack(), OpponentRack: g.OpponentRack(), Unseen: g.Unseen(),
		Status: g.Status(), Moves: moves(g), Clocks: m.Clocks(), Paused: m.Paused(),
		Result: g.Result(), BotError: m.BotError(),
	}
}

func moves(g *classic.Game) []*classic.Move {
	n := g.MoveCount()
	list := make([]*classic.Move, 0, n)
	for i := 0; i < n; i++ {
		list = append(list, g.MoveAt(i))
	}
	return list
}
