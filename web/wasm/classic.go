//go:build js && wasm

package main

import (
	"encoding/json"

	"lexico/engine/modes/classic"
)

type classicMatch = *classic.Match

func registerClassic() {
	handlers["classicNew"] = classicNew
	handlers["classicNewLocal"] = classicNewLocal
	handlers["classicLoad"] = classicLoad
	handlers["classicMirror"] = classicMirror
	handlers["classicRead"] = classicRead
	handlers["classicClocks"] = classicClocks
	handlers["classicPlay"] = classicPlay
	handlers["classicPause"] = classicPause
	handlers["classicResume"] = classicResume
	handlers["classicSave"] = classicSave
	handlers["classicProgress"] = classicProgress
	handlers["classicView"] = classicView
	handlers["classicClose"] = closeAny
	handlers["endgameNew"] = endgameNew
}

func endgameNew(args []json.RawMessage) (any, error) {
	search := classic.NewEndgameSearch(int(number(args, 0)), int(number(args, 1)), int(number(args, 2)), int(number(args, 3)), text(args, 4))
	return open(func(l *listener) (classicMatch, error) {
		return search.Match(number(args, 5), number(args, 6), flag(args, 7), l)
	})
}

func classicNew(args []json.RawMessage) (any, error) {
	return open(func(l *listener) (classicMatch, error) {
		return classic.NewMatch(text(args, 0), int(number(args, 1)), number(args, 2), number(args, 3), flag(args, 4), l)
	})
}

func classicNewLocal(args []json.RawMessage) (any, error) {
	return open(func(l *listener) (classicMatch, error) {
		return classic.NewLocalMatch(text(args, 0), text(args, 1), int(number(args, 2)), number(args, 3), number(args, 4), flag(args, 5), l)
	})
}

func classicLoad(args []json.RawMessage) (any, error) {
	return open(func(l *listener) (classicMatch, error) {
		return classic.LoadMatch(text(args, 0), l)
	})
}

func classicMirror(args []json.RawMessage) (any, error) {
	return open(func(l *listener) (classicMatch, error) {
		if flag(args, 2) {
			return classic.LoadWatch(text(args, 0), flag(args, 1), l)
		}
		return classic.LoadMirror(text(args, 0), flag(args, 1), l)
	})
}

func classicRead(args []json.RawMessage) (any, error) {
	return act(args, func(m classicMatch) (any, error) { return read(m), nil })
}

func classicClocks(args []json.RawMessage) (any, error) {
	return act(args, func(m classicMatch) (any, error) { return m.Clocks(), nil })
}

func classicPlay(args []json.RawMessage) (any, error) {
	return act(args, func(m classicMatch) (any, error) {
		_, err := m.Play(text(args, 1))
		return true, err
	})
}

func classicPause(args []json.RawMessage) (any, error) {
	return act(args, func(m classicMatch) (any, error) { m.Pause(); return true, nil })
}

func classicResume(args []json.RawMessage) (any, error) {
	return act(args, func(m classicMatch) (any, error) { m.Resume(); return true, nil })
}

func classicSave(args []json.RawMessage) (any, error) {
	return act(args, func(m classicMatch) (any, error) { return m.Save() })
}

func classicProgress(args []json.RawMessage) (any, error) {
	return classic.ReadProgress(text(args, 0))
}

func classicView(args []json.RawMessage) (any, error) {
	return act(args, func(m classicMatch) (any, error) { return m.ViewFor(flag(args, 1)) })
}
