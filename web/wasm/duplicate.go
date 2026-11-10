//go:build js && wasm

package main

import (
	"encoding/json"

	"lexico/engine/modes/duplicate"
)

type duplicateMatch = *duplicate.Match

type duplicateState struct {
	Board       string
	Unseen      string
	Phase       string
	Rack        string
	InvalidRack string
	Proposal    *duplicate.Attempt
	TurnMs      int64
	RemainingMs int64
	CancelMs    int64
	LastError   string
	Paused      bool
	Status      *duplicate.Status
	Turns       []*duplicate.Turn
	Result      *duplicate.Result
	RevealAtEnd bool
	SingleCheck bool
	MaxRounds   int
}

func registerDuplicate() {
	handlers["duplicateNew"] = duplicateNew
	handlers["duplicateLoad"] = duplicateLoad
	handlers["duplicateRead"] = duplicateRead
	handlers["duplicateShowRack"] = duplicateShowRack
	handlers["duplicatePropose"] = duplicatePropose
	handlers["duplicateCancel"] = duplicateCancel
	handlers["duplicatePause"] = duplicatePause
	handlers["duplicateResume"] = duplicateResume
	handlers["duplicateSave"] = duplicateSave
	handlers["duplicateClose"] = closeAny
}

func duplicateNew(args []json.RawMessage) (any, error) {
	return open(func(l *listener) (duplicateMatch, error) {
		return duplicate.NewMatch(number(args, 0), flag(args, 1), flag(args, 2), int(number(args, 3)), l)
	})
}

func duplicateLoad(args []json.RawMessage) (any, error) {
	return open(func(l *listener) (duplicateMatch, error) { return duplicate.LoadMatch(text(args, 0), l) })
}

func duplicateRead(args []json.RawMessage) (any, error) {
	return act(args, func(m duplicateMatch) (any, error) { return readDuplicate(m), nil })
}

func duplicateShowRack(args []json.RawMessage) (any, error) {
	return act(args, func(m duplicateMatch) (any, error) { return true, m.ShowRack() })
}

func duplicatePropose(args []json.RawMessage) (any, error) {
	return act(args, func(m duplicateMatch) (any, error) { return m.Propose(text(args, 1)) })
}

func duplicateCancel(args []json.RawMessage) (any, error) {
	return act(args, func(m duplicateMatch) (any, error) { m.Cancel(); return true, nil })
}

func duplicatePause(args []json.RawMessage) (any, error) {
	return act(args, func(m duplicateMatch) (any, error) { m.Pause(); return true, nil })
}

func duplicateResume(args []json.RawMessage) (any, error) {
	return act(args, func(m duplicateMatch) (any, error) { m.Resume(); return true, nil })
}

func duplicateSave(args []json.RawMessage) (any, error) {
	return act(args, func(m duplicateMatch) (any, error) { return m.Save() })
}

func readDuplicate(m duplicateMatch) duplicateState {
	g := m.Game()
	return duplicateState{
		Board: g.Board(), Unseen: g.Unseen(), Phase: m.Phase(), Rack: m.Rack(), InvalidRack: m.InvalidRack(),
		Proposal: m.Proposal(), TurnMs: m.TurnMs(), RemainingMs: m.TurnRemainingMs(), CancelMs: m.CancelRemainingMs(),
		LastError: m.LastError(), Paused: m.Paused(), Status: g.Status(), Turns: duplicateTurns(g), Result: g.Result(),
		RevealAtEnd: g.RevealAtEnd(), SingleCheck: g.InvalidPlayLosesTurn(), MaxRounds: g.MaxRounds(),
	}
}

func duplicateTurns(g *duplicate.Game) []*duplicate.Turn {
	turns := make([]*duplicate.Turn, 0, g.TurnCount())
	for i := 0; i < g.TurnCount(); i++ {
		turns = append(turns, g.TurnAt(i))
	}
	return turns
}
