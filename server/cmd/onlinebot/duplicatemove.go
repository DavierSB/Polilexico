package main

import (
	"encoding/json"
	"log"
	"strings"

	"lexico/engine"
)

type scoredPlay struct {
	Description string `json:"description"`
	Score       int    `json:"score"`
}

func (b *bot) duplicateMove(board, rack string) string {
	in := b.choose(board, rack)
	b.fallback = topPlay(board, rack)
	if isExchange(in) {
		return b.fallback
	}
	return in
}

func topPlay(board, rack string) string {
	best := scoredPlay{Score: -1}
	for _, p := range scoredPlays(board, rack) {
		if !isExchange(input(candidate{p.Description})) && p.Score > best.Score {
			best = p
		}
	}
	return input(candidate{best.Description})
}

func scoredPlays(board, rack string) []scoredPlay {
	text, err := engine.BestMoves(board, engineRack(rack), 80)
	if err != nil {
		log.Printf("motor: %v", err)
		return nil
	}
	var ps []scoredPlay
	json.Unmarshal([]byte(text), &ps)
	return ps
}

func isExchange(in string) bool {
	return strings.HasPrefix(in, "cambiar ")
}
