//go:build js && wasm

package main

import (
	"encoding/json"
	"sync"
	"syscall/js"
	"time"

	"lexico/engine/review"
)

const (
	explorePass = "pasar"
	pushGap     = 150 * time.Millisecond
)

type exploration struct {
	mu       sync.Mutex
	explorer *review.Explorer
	listener *listener
	analysis *review.Analysis
	done     bool
	seq      int
}

type explorePosition struct {
	Board  string
	Racks  [2]string
	Scores [2]int
	OnTurn int
	Over   bool
	Path   []*review.Move
	Ending *review.EndgameLine
}

type exploreLines struct {
	Seq       int
	Depth     int
	Exact     bool
	Analyzing bool
	Lines     []endgameLine
}

func registerExplore() {
	handlers["exploreReview"] = exploreReview
	handlers["explorePosition"] = explorePositionTurn
	handlers["exploreState"] = func(args []json.RawMessage) (any, error) { return act(args, (*exploration).state) }
	handlers["explorePlay"] = func(args []json.RawMessage) (any, error) {
		return act(args, func(e *exploration) (any, error) { return e.play(text(args, 1)) })
	}
	handlers["exploreBack"] = func(args []json.RawMessage) (any, error) {
		return act(args, func(e *exploration) (any, error) { return e.back(int(number(args, 1))) })
	}
	handlers["exploreAnalyze"] = func(args []json.RawMessage) (any, error) {
		return act(args, func(e *exploration) (any, error) { return e.analyze(int(number(args, 1))) })
	}
	handlers["exploreLines"] = func(args []json.RawMessage) (any, error) { return act(args, (*exploration).lines) }
	handlers["exploreClose"] = closeAny
}

func exploreReview(args []json.RawMessage) (any, error) {
	g, err := review.Parse(text(args, 0))
	if err != nil {
		return nil, err
	}
	t := g.TurnAt(int(number(args, 1)))
	if t == nil {
		return nil, errNoTurn
	}
	return exploreTurn(t)
}

func explorePositionTurn(args []json.RawMessage) (any, error) {
	t, err := positionTurn(args)
	if err != nil {
		return nil, err
	}
	return exploreTurn(t)
}

func exploreTurn(t *review.Turn) (any, error) {
	return open(func(l *listener) (*exploration, error) {
		x, err := t.Explore()
		if err != nil {
			return nil, err
		}
		return &exploration{explorer: x, listener: l}, nil
	})
}

func (e *exploration) state() (any, error) {
	x := e.explorer
	p := explorePosition{Board: x.Board(), OnTurn: x.OnTurn(), Over: x.Over(), Ending: x.Ending()}
	for i := range 2 {
		p.Racks[i], p.Scores[i] = x.Rack(i), x.Score(i)
	}
	for i := 0; i < x.StepCount(); i++ {
		p.Path = append(p.Path, x.StepAt(i))
	}
	return p, nil
}

func (e *exploration) play(input string) (any, error) {
	e.stop()
	if input == "" {
		input = explorePass
	}
	if _, err := e.explorer.Play(input); err != nil {
		return err.Error(), nil
	}
	return "", nil
}

func (e *exploration) back(steps int) (any, error) {
	e.stop()
	for range max(steps, 1) {
		if e.explorer.Back() != nil {
			break
		}
	}
	return true, nil
}

func (e *exploration) analyze(budgetMs int) (any, error) {
	e.stop()
	a, err := e.explorer.Analysis(budgetMs)
	if err != nil || a == nil {
		return false, err
	}
	e.mu.Lock()
	e.seq++
	seq := e.seq
	e.analysis, e.done = a, false
	e.mu.Unlock()
	go e.run(a, seq)
	return seq, nil
}

type pusher struct {
	e    *exploration
	a    *review.Analysis
	seq  int
	last time.Time
}

func (p *pusher) OnChange() {
	if time.Since(p.last) < pushGap {
		return
	}
	p.last = time.Now()
	p.e.push(p.a, p.seq, false)
}

func (e *exploration) run(a *review.Analysis, seq int) {
	_ = a.Run(&pusher{e: e, a: a, seq: seq})
	e.mu.Lock()
	current := e.analysis == a
	if current {
		e.done = true
	}
	e.mu.Unlock()
	if current {
		e.push(a, seq, true)
	}
}

func (e *exploration) push(a *review.Analysis, seq int, done bool) {
	text, err := json.Marshal(linesOf(a, seq, done))
	if err == nil {
		js.Global().Call("lexicoChanged", e.listener.id, string(text))
	}
}

func (e *exploration) lines() (any, error) {
	e.mu.Lock()
	a, done, seq := e.analysis, e.done, e.seq
	e.mu.Unlock()
	if a == nil {
		return exploreLines{}, nil
	}
	return linesOf(a, seq, done), nil
}

func linesOf(a *review.Analysis, seq int, done bool) exploreLines {
	latest := a.Latest()
	view := exploreLines{Seq: seq, Depth: latest.Depth, Exact: latest.Exact, Analyzing: !done}
	for i := 0; i < latest.LineCount(); i++ {
		l := latest.LineAt(i)
		line := endgameLine{EndgameLine: l}
		for k := 0; k < l.StepCount(); k++ {
			line.Steps = append(line.Steps, l.StepAt(k))
		}
		view.Lines = append(view.Lines, line)
	}
	return view
}

func (e *exploration) stop() {
	e.mu.Lock()
	a := e.analysis
	e.analysis = nil
	e.mu.Unlock()
	if a != nil {
		a.Stop()
	}
}

func (e *exploration) Close() {
	e.stop()
}
