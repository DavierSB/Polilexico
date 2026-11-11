package review

import (
	"slices"
	"strings"

	"github.com/domino14/macondo/game"
	macondo "github.com/domino14/macondo/move"

	"lexico/engine/internal/core"
)

const (
	allPlays       = 1 << 20
	exchangePrefix = "(Cambiar "
	exchangeSuffix = ")"
)

func (t *Turn) Rank() (*Turn, error) {
	plays, err := t.allPlays()
	if err != nil {
		return nil, err
	}
	ranked := *t
	ranked.candidates = rankedMoves(plays, !t.master)
	ranked.marks = ranked.remark(t.marks)
	return &ranked, nil
}

func (t *Turn) allPlays() ([]*macondo.Move, error) {
	g, err := t.game()
	if err != nil {
		return nil, err
	}
	if t.master {
		return masterPlays(g)
	}
	hasty, err := core.NewBot(g, core.DefaultBot)
	if err != nil {
		return nil, err
	}
	return hasty.GenerateMoves(allPlays), nil
}

func (t *Turn) game() (*game.Game, error) {
	rows, err := core.ParseBoard(t.Board)
	if err != nil {
		return nil, err
	}
	return core.NewGameFromBoard(rows, t.position.rack)
}

func masterPlays(g *game.Game) ([]*macondo.Move, error) {
	master, err := core.NewMasterBot(g)
	if err != nil {
		return nil, err
	}
	return core.MasterPlays(master, g, 0), nil
}

func rankedMoves(plays []*macondo.Move, hasEquity bool) []*Move {
	out := make([]*Move, len(plays))
	for i, m := range plays {
		out[i] = &Move{Description: describe(m), Score: m.Score(), Equity: m.Equity(), HasEquity: hasEquity}
	}
	return out
}

func (t *Turn) remark(marks []*Mark) []*Mark {
	out := make([]*Mark, len(marks))
	for i, m := range marks {
		out[i] = t.mark(m.Who, &Move{Description: m.Description, Score: m.Score})
	}
	return out
}

func sameMove(a, b string) bool {
	return canonical(a) == canonical(b)
}

func canonical(description string) string {
	tiles, ok := exchanged(description)
	if !ok {
		return description
	}
	slices.Sort(tiles)
	return exchangePrefix + strings.Join(tiles, "") + exchangeSuffix
}

func exchanged(description string) ([]string, bool) {
	inner, ok := strings.CutPrefix(description, exchangePrefix)
	if !ok {
		return nil, false
	}
	inner, ok = strings.CutSuffix(inner, exchangeSuffix)
	return tokens(inner), ok
}
