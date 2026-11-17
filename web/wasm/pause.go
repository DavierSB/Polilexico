//go:build js && wasm

package main

import (
	"time"

	"lexico/engine/review"
)

const (
	pauseGap   = 100 * time.Millisecond
	pauseCheck = 256
)

func registerPause() {
	review.SetPause(pauser())
}

func pauser() func() {
	calls, last := 0, time.Now()
	return func() {
		if calls++; calls%pauseCheck != 0 || time.Since(last) < pauseGap {
			return
		}
		time.Sleep(time.Millisecond)
		last = time.Now()
	}
}
