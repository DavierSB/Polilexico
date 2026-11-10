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
	handlers["demoScored"] = demoScored
	handlers["areWords"] = areWords
	handlers["anagrams"] = anagrams
	handlers["anagramTiles"] = anagramTiles
	handlers["randomWord"] = randomWord
}

func anagrams(args []json.RawMessage) (any, error) {
	return engine.Anagrams(text(args, 0))
}

func anagramTiles(args []json.RawMessage) (any, error) {
	return engine.AnagramTiles(text(args, 0))
}

func randomWord(args []json.RawMessage) (any, error) {
	return engine.RandomWord(int(number(args, 0)), int(number(args, 1)))
}

func demoScored(_ []json.RawMessage) (any, error) {
	game, err := demo.PlayWithScores()
	return json.RawMessage(game), err
}

func areWords(args []json.RawMessage) (any, error) {
	var words []string
	at(args, 0, &words)
	valid := make([]bool, len(words))
	for i, w := range words {
		ok, err := engine.IsValidWord(w)
		valid[i] = ok && err == nil
	}
	return valid, nil
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
