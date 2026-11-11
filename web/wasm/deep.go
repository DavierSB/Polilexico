//go:build js && wasm

package main

import (
	"encoding/json"
	"errors"

	"lexico/engine/review"
)

var errNotDeep = errors.New("esta posición no es de pre-final ni de final")

type quiet struct{}

func (quiet) OnChange() {}

type endgameLine struct {
	*review.EndgameLine
	Steps []*review.Move
}

type endgameView struct {
	Depth int
	Exact bool
	Lines []endgameLine
}

type preEndgameView struct {
	*review.PreEndgame
	Moves []*review.PreEndgameMove
}

func registerDeep() {
	handlers["positionPhase"] = positionPhase
	handlers["positionDeep"] = positionDeep
	handlers["reviewDeep"] = reviewDeep
}

func positionTurn(args []json.RawMessage) (*review.Turn, error) {
	return review.FromPosition(text(args, 0), text(args, 1), int(number(args, 2)), int(number(args, 3)))
}

func positionPhase(args []json.RawMessage) (any, error) {
	t, err := positionTurn(args)
	if err != nil {
		return nil, err
	}
	return phaseOf(t), nil
}

func phaseOf(t *review.Turn) string {
	switch {
	case t.Endgame:
		return "endgame"
	case t.PreEndgame:
		return "preendgame"
	}
	return "midgame"
}

func positionDeep(args []json.RawMessage) (any, error) {
	t, err := positionTurn(args)
	if err != nil {
		return nil, err
	}
	return deepOf(t, int(number(args, 4)))
}

func reviewDeep(args []json.RawMessage) (any, error) {
	g, err := review.Parse(text(args, 0))
	if err != nil {
		return nil, err
	}
	t := g.TurnAt(int(number(args, 1)))
	if t == nil {
		return nil, errNoTurn
	}
	return deepOf(t, int(number(args, 2)))
}

func deepOf(t *review.Turn, budgetMs int) (any, error) {
	if t.Endgame {
		return endgameOf(t, budgetMs)
	}
	if t.PreEndgame {
		return preEndgameOf(t)
	}
	return nil, errNotDeep
}

func endgameOf(t *review.Turn, budgetMs int) (any, error) {
	x, err := t.Explore()
	if err != nil {
		return nil, err
	}
	a, err := x.Analysis(budgetMs)
	if err != nil || a == nil {
		return endgameView{}, err
	}
	if err := a.Run(quiet{}); err != nil {
		return nil, err
	}
	e := a.Latest()
	view := endgameView{Depth: e.Depth, Exact: e.Exact}
	for i := 0; i < e.LineCount(); i++ {
		l := e.LineAt(i)
		line := endgameLine{EndgameLine: l}
		for k := 0; k < l.StepCount(); k++ {
			line.Steps = append(line.Steps, l.StepAt(k))
		}
		view.Lines = append(view.Lines, line)
	}
	return view, nil
}

func preEndgameOf(t *review.Turn) (any, error) {
	a, err := t.AnalyzePreEndgame()
	if err != nil {
		return nil, err
	}
	if err := a.Run(quiet{}); err != nil {
		return nil, err
	}
	p := a.Latest()
	view := preEndgameView{PreEndgame: p}
	for i := 0; i < p.MoveCount(); i++ {
		view.Moves = append(view.Moves, p.MoveAt(i))
	}
	return view, nil
}
