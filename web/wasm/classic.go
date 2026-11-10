//go:build js && wasm

package main

import (
	"encoding/json"
	"errors"
	"sync"

	"lexico/engine/modes/classic"
)

var errNoMatch = errors.New("la partida ya no existe")

var (
	matchesMu sync.Mutex
	matches   = map[int]*classic.Match{}
	lastMatch = 0
)

type listener struct{ id int }

func (l *listener) OnChange() {
	changed(l.id)
}

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
	handlers["classicView"] = classicView
	handlers["classicClose"] = classicClose
}

func classicNew(args []json.RawMessage) (any, error) {
	return open(func(l *listener) (*classic.Match, error) {
		return classic.NewMatch(text(args, 0), int(number(args, 1)), number(args, 2), number(args, 3), flag(args, 4), l)
	})
}

func classicNewLocal(args []json.RawMessage) (any, error) {
	return open(func(l *listener) (*classic.Match, error) {
		return classic.NewLocalMatch(text(args, 0), text(args, 1), int(number(args, 2)), number(args, 3), number(args, 4), flag(args, 5), l)
	})
}

func classicLoad(args []json.RawMessage) (any, error) {
	return open(func(l *listener) (*classic.Match, error) {
		return classic.LoadMatch(text(args, 0), l)
	})
}

func classicMirror(args []json.RawMessage) (any, error) {
	return open(func(l *listener) (*classic.Match, error) {
		if flag(args, 2) {
			return classic.LoadWatch(text(args, 0), flag(args, 1), l)
		}
		return classic.LoadMirror(text(args, 0), flag(args, 1), l)
	})
}

func classicRead(args []json.RawMessage) (any, error) {
	m, err := find(args)
	if err != nil {
		return nil, err
	}
	return read(m), nil
}

func classicClocks(args []json.RawMessage) (any, error) {
	m, err := find(args)
	if err != nil {
		return nil, err
	}
	return m.Clocks(), nil
}

func classicPlay(args []json.RawMessage) (any, error) {
	m, err := find(args)
	if err != nil {
		return nil, err
	}
	_, err = m.Play(text(args, 1))
	return true, err
}

func classicPause(args []json.RawMessage) (any, error) {
	return control(args, (*classic.Match).Pause)
}

func classicResume(args []json.RawMessage) (any, error) {
	return control(args, (*classic.Match).Resume)
}

func classicSave(args []json.RawMessage) (any, error) {
	m, err := find(args)
	if err != nil {
		return nil, err
	}
	return m.Save()
}

func classicView(args []json.RawMessage) (any, error) {
	m, err := find(args)
	if err != nil {
		return nil, err
	}
	return m.ViewFor(flag(args, 1))
}

func classicClose(args []json.RawMessage) (any, error) {
	m, err := find(args)
	if err != nil {
		return true, nil
	}
	forget(int(number(args, 0)))
	m.Close()
	return true, nil
}

func control(args []json.RawMessage, act func(*classic.Match)) (any, error) {
	m, err := find(args)
	if err != nil {
		return nil, err
	}
	act(m)
	return true, nil
}

func open(create func(*listener) (*classic.Match, error)) (any, error) {
	matchesMu.Lock()
	lastMatch++
	l := &listener{lastMatch}
	matchesMu.Unlock()
	m, err := create(l)
	if err != nil {
		return nil, err
	}
	matchesMu.Lock()
	matches[l.id] = m
	matchesMu.Unlock()
	return l.id, nil
}

func find(args []json.RawMessage) (*classic.Match, error) {
	matchesMu.Lock()
	defer matchesMu.Unlock()
	m, ok := matches[int(number(args, 0))]
	if !ok {
		return nil, errNoMatch
	}
	return m, nil
}

func forget(id int) {
	matchesMu.Lock()
	defer matchesMu.Unlock()
	delete(matches, id)
}
