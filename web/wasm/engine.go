//go:build js && wasm

package main

import (
	"encoding/json"

	"lexico/engine"
	"lexico/engine/modes/demo"
)

func registerEngine() {
	handlers["init"] = initEngine
	handlers["isValidWord"] = isValidWord
	handlers["placementScore"] = placementScore
	handlers["bestMoves"] = bestMoves
	handlers["demo"] = demoGame
}

func initEngine(args []json.RawMessage) (any, error) {
	return true, engine.Init(text(args, 0), text(args, 1))
}

func isValidWord(args []json.RawMessage) (any, error) {
	return engine.IsValidWord(text(args, 0))
}

func placementScore(args []json.RawMessage) (any, error) {
	return engine.PlacementScore(text(args, 0), text(args, 1))
}

func bestMoves(args []json.RawMessage) (any, error) {
	found, err := engine.BestMoves(text(args, 0), text(args, 1), int(number(args, 2)))
	return json.RawMessage(found), err
}

func demoGame(_ []json.RawMessage) (any, error) {
	game, err := demo.Play()
	return json.RawMessage(game), err
}
