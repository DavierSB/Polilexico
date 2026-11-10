//go:build js && wasm

package main

import (
	"encoding/json"
	"fmt"
)

func errUnknown(name string) error {
	return fmt.Errorf("llamada desconocida: %s", name)
}

func text(args []json.RawMessage, i int) string {
	var s string
	at(args, i, &s)
	return s
}

func number(args []json.RawMessage, i int) int64 {
	var n int64
	at(args, i, &n)
	return n
}

func flag(args []json.RawMessage, i int) bool {
	var b bool
	at(args, i, &b)
	return b
}

func at(args []json.RawMessage, i int, into any) {
	if i < len(args) {
		_ = json.Unmarshal(args[i], into)
	}
}
