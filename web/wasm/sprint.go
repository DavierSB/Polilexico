//go:build js && wasm

package main

import (
	"encoding/json"

	"lexico/engine/modes/sprint"
)

type sprintMatch = *sprint.Match

type sprintState struct {
	Phase       string
	Paused      bool
	Lives       int
	MaxLives    int
	Solved      int
	Posed       int
	TotalMs     int64
	RemainingMs int64
	Board       string
	Rack        string
	Bingos      int
	Solutions   []*sprint.Solution
	Outcome     string
	Answer      *sprint.Solution
	LastError   string
	CueCount    int
	Cue         string
}

func registerSprint() {
	handlers["sprintNew"] = sprintNew
	handlers["sprintRead"] = sprintRead
	handlers["sprintPropose"] = sprintPropose
	handlers["sprintGiveUp"] = sprintGiveUp
	handlers["sprintNext"] = sprintNext
	handlers["sprintPause"] = sprintPause
	handlers["sprintResume"] = sprintResume
	handlers["sprintClose"] = closeAny
}

func sprintNew(args []json.RawMessage) (any, error) {
	return open(func(l *listener) (sprintMatch, error) {
		return sprint.NewMatch(number(args, 0), int(number(args, 1)), flag(args, 2), text(args, 3), l)
	})
}

func sprintRead(args []json.RawMessage) (any, error) {
	return act(args, func(m sprintMatch) (any, error) { return readSprint(m), nil })
}

func sprintPropose(args []json.RawMessage) (any, error) {
	return act(args, func(m sprintMatch) (any, error) { return m.Propose(text(args, 1)) })
}

func sprintGiveUp(args []json.RawMessage) (any, error) {
	return act(args, func(m sprintMatch) (any, error) { return true, m.GiveUp() })
}

func sprintNext(args []json.RawMessage) (any, error) {
	return act(args, func(m sprintMatch) (any, error) { m.Next(); return true, nil })
}

func sprintPause(args []json.RawMessage) (any, error) {
	return act(args, func(m sprintMatch) (any, error) { m.Pause(); return true, nil })
}

func sprintResume(args []json.RawMessage) (any, error) {
	return act(args, func(m sprintMatch) (any, error) { m.Resume(); return true, nil })
}

func readSprint(m sprintMatch) sprintState {
	return sprintState{
		Phase: m.Phase(), Paused: m.Paused(), Lives: m.Lives(), MaxLives: m.MaxLives(), Solved: m.Solved(), Posed: m.Posed(),
		TotalMs: m.TotalMs(), RemainingMs: m.RemainingMs(), Board: m.Board(), Rack: m.Rack(), Bingos: m.SolutionCount(),
		Solutions: sprintSolutions(m), Outcome: m.Outcome(), Answer: m.Answer(), LastError: m.LastError(),
		CueCount: m.CueCount(), Cue: m.Cue(),
	}
}

func sprintSolutions(m sprintMatch) []*sprint.Solution {
	list := []*sprint.Solution{}
	for i := 0; i < m.SolutionCount(); i++ {
		if s := m.SolutionAt(i); s != nil {
			list = append(list, s)
		}
	}
	return list
}
