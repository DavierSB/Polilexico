//go:build js && wasm

package main

import (
	"encoding/json"
	"errors"

	"lexico/engine/review"
)

var errNoTurn = errors.New("ese turno no existe")

type reviewTurn struct {
	*review.Turn
	Candidates []*review.Move
	Marks      []*review.Mark
}

type reviewGame struct {
	*review.Game
	Turns []reviewTurn
}

func registerReview() {
	handlers["reviewOpen"] = reviewOpen
	handlers["reviewRank"] = reviewRank
}

func reviewOpen(args []json.RawMessage) (any, error) {
	g, err := review.Parse(text(args, 0))
	if err != nil {
		return nil, err
	}
	turns := make([]reviewTurn, 0, g.TurnCount())
	for i := 0; i < g.TurnCount(); i++ {
		turns = append(turns, turnOf(g.TurnAt(i)))
	}
	return reviewGame{g, turns}, nil
}

func reviewRank(args []json.RawMessage) (any, error) {
	g, err := review.Parse(text(args, 0))
	if err != nil {
		return nil, err
	}
	t := g.TurnAt(int(number(args, 1)))
	if t == nil {
		return nil, errNoTurn
	}
	ranked, err := t.Rank()
	if err != nil {
		return nil, err
	}
	return turnOf(ranked), nil
}

func turnOf(t *review.Turn) reviewTurn {
	out := reviewTurn{Turn: t}
	for i := 0; i < t.CandidateCount(); i++ {
		out.Candidates = append(out.Candidates, t.CandidateAt(i))
	}
	for i := 0; i < t.MarkCount(); i++ {
		out.Marks = append(out.Marks, t.MarkAt(i))
	}
	return out
}
