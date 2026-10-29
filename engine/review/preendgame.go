package review

import (
	"errors"
	"sync"
	"sync/atomic"

	"lexico/engine/events"
	"lexico/engine/internal/core"
	"lexico/engine/internal/preendgame"
)

const preEndgameMoves = 15

const (
	VerdictBingo = "bingo"
	VerdictPass  = "pass"
)

var errNoPreEndgame = errors.New("este turno no es de pre-final")

type PreEndgame struct {
	Done       bool
	Verdict    string
	PlayedRank int
	Played     *PreEndgameMove
	moves      []*PreEndgameMove
}

type PreEndgameMove struct {
	Description string
	Score       int
	Equity      float64
	Adjustment  float64
	Value       float64
	Verdict     string
}

type PreEndgameAnalysis struct {
	turn    *Turn
	mu      sync.Mutex
	latest  *PreEndgame
	stopped atomic.Bool
}

func (p *PreEndgame) MoveCount() int {
	return len(p.moves)
}

func (p *PreEndgame) MoveAt(i int) *PreEndgameMove {
	if i < 0 || i >= len(p.moves) {
		return nil
	}
	return p.moves[i]
}

func (t *Turn) AnalyzePreEndgame() (*PreEndgameAnalysis, error) {
	if !t.PreEndgame {
		return nil, errNoPreEndgame
	}
	return &PreEndgameAnalysis{turn: t, latest: &PreEndgame{PlayedRank: -1}}, nil
}

func (a *PreEndgameAnalysis) Run(l events.Listener) error {
	analysis, err := a.turn.preEndgameAnalysis()
	if err != nil {
		return err
	}
	ranking, played := analysis.Ranking(preEndgameMoves), analysis.Find(a.turn.isPlayed)
	a.publish(preEndgameOf(ranking, nil, played, false), l)
	if a.stopped.Load() {
		return nil
	}
	a.publish(preEndgameOf(ranking, analysis.Verdict(), played, true), l)
	return nil
}

func (a *PreEndgameAnalysis) Stop() {
	a.stopped.Store(true)
}

func (a *PreEndgameAnalysis) Latest() *PreEndgame {
	a.mu.Lock()
	defer a.mu.Unlock()
	return a.latest
}

func (a *PreEndgameAnalysis) publish(p *PreEndgame, l events.Listener) {
	a.mu.Lock()
	a.latest = p
	a.mu.Unlock()
	l.OnChange()
}

func (t *Turn) preEndgameAnalysis() (*preendgame.Analysis, error) {
	rows, err := core.ParseBoard(t.Board)
	if err != nil {
		return nil, err
	}
	g, err := core.NewPreEndgame(rows, t.position.rack, t.position.scores)
	if err != nil {
		return nil, err
	}
	return preendgame.PrepareFrom(g, t.drawBase).Analyze(preendgame.Default)
}

func preEndgameOf(ranking []preendgame.Rated, verdict, played *preendgame.Rated, done bool) *PreEndgame {
	p := &PreEndgame{Done: done, moves: rows(ranking, verdict), PlayedRank: -1}
	if verdict != nil {
		p.Verdict = verdictName(verdict.Verdict)
	}
	if played != nil {
		p.Played = rowOf(*played)
		p.PlayedRank = rankOf(p.moves, p.Played.Description)
	}
	return p
}

func rows(ranking []preendgame.Rated, verdict *preendgame.Rated) []*PreEndgameMove {
	var out []*PreEndgameMove
	if verdict != nil {
		out = append(out, rowOf(*verdict))
	}
	for _, r := range ranking {
		if row := rowOf(r); rankOf(out, row.Description) < 0 {
			out = append(out, row)
		}
	}
	return out[:min(len(out), preEndgameMoves)]
}

func rowOf(r preendgame.Rated) *PreEndgameMove {
	return &PreEndgameMove{Description: describe(r.Move), Score: r.Move.Score(), Equity: r.Move.Equity(), Adjustment: r.Adjustment,
		Value: r.Value(), Verdict: verdictName(r.Verdict)}
}

func rankOf(moves []*PreEndgameMove, description string) int {
	for i, m := range moves {
		if m.Description == description {
			return i
		}
	}
	return -1
}

func verdictName(v preendgame.Verdict) string {
	switch v {
	case preendgame.BingoWins:
		return VerdictBingo
	case preendgame.PassWins:
		return VerdictPass
	}
	return ""
}
