//go:build js && wasm

package main

import (
	"encoding/json"
	"errors"
	"sync"
)

var errNoMatch = errors.New("la partida ya no existe")

type closer interface{ Close() }

var (
	registryMu sync.Mutex
	registered = map[int]closer{}
	lastID     = 0
)

type listener struct{ id int }

func (l *listener) OnChange() {
	changed(l.id)
}

func open[T closer](create func(*listener) (T, error)) (any, error) {
	registryMu.Lock()
	lastID++
	l := &listener{lastID}
	registryMu.Unlock()
	item, err := create(l)
	if err != nil {
		return nil, err
	}
	registryMu.Lock()
	registered[l.id] = item
	registryMu.Unlock()
	return l.id, nil
}

func find[T closer](args []json.RawMessage) (T, error) {
	registryMu.Lock()
	defer registryMu.Unlock()
	item, ok := registered[int(number(args, 0))].(T)
	if !ok {
		return item, errNoMatch
	}
	return item, nil
}

func closeAny(args []json.RawMessage) (any, error) {
	id := int(number(args, 0))
	registryMu.Lock()
	item, ok := registered[id]
	delete(registered, id)
	registryMu.Unlock()
	if ok {
		item.Close()
	}
	return true, nil
}

func act[T closer](args []json.RawMessage, do func(T) (any, error)) (any, error) {
	item, err := find[T](args)
	if err != nil {
		return nil, err
	}
	return do(item)
}
