package review

import (
	"errors"
	"sync"
	"time"

	"github.com/domino14/macondo/game"
	pb "github.com/domino14/macondo/gen/api/proto/macondo"
	macondo "github.com/domino14/macondo/move"

	"lexico/engine/events"
	"lexico/engine/internal/core"
	"lexico/engine/internal/endgame"
)

var errNoMoves = errors.New("no hay jugadas que deshacer")

type Explorer struct {
	mu     sync.Mutex
	start  *game.Game
	game   *game.Game
	played []*macondo.Move
	keep   func(*macondo.Move) bool
}

type Analysis struct {
	analyzer *endgame.Analyzer
	base     *game.Game
	keep     func(*macondo.Move) bool
}

func (t *Turn) Explore() (*Explorer, error) {
	if !t.Endgame {
		return nil, errNoEndgame
	}
	g, err := t.endgameGame()
	if err != nil {
		return nil, err
	}
	g.SetEndgameMode(true)
	return &Explorer{start: g, game: g.Copy(), keep: t.isPlayed}, nil
}

func (x *Explorer) Play(input string) (*Move, error) {
	x.mu.Lock()
	defer x.mu.Unlock()
	m, err := core.ParseInput(x.game, x.game.PlayerOnTurn(), input)
	if err != nil {
		return nil, err
	}
	if err := x.game.PlayMove(m, false, 0); err != nil {
		return nil, err
	}
	x.played = append(x.played, m)
	return moveOf(m), nil
}

func (x *Explorer) Back() error {
	x.mu.Lock()
	defer x.mu.Unlock()
	if len(x.played) == 0 {
		return errNoMoves
	}
	x.played = x.played[:len(x.played)-1]
	x.game = replayed(x.start, x.played)
	return nil
}

func (x *Explorer) Analysis(budgetMs int) (*Analysis, error) {
	x.mu.Lock()
	defer x.mu.Unlock()
	if over(x.game) {
		return nil, nil
	}
	c := endgame.Default
	c.Budget = time.Duration(budgetMs) * time.Millisecond
	base := x.game.Copy()
	z, err := endgame.NewAnalyzer(base, c)
	if err != nil {
		return nil, err
	}
	return &Analysis{analyzer: z, base: base, keep: x.rootKeep()}, nil
}

func (a *Analysis) Run(l events.Listener) error {
	_, err := a.analyzer.Run(endgameLines, a.keep, l.OnChange)
	return err
}

func (a *Analysis) Stop() {
	a.analyzer.Stop()
}

func (a *Analysis) Latest() *Endgame {
	return endgameOf(a.base, a.analyzer.Latest())
}

func (x *Explorer) Board() string {
	x.mu.Lock()
	defer x.mu.Unlock()
	return core.BoardText(x.game)
}

func (x *Explorer) Rack(player int) string {
	x.mu.Lock()
	defer x.mu.Unlock()
	return core.RackText(x.game, player)
}

func (x *Explorer) Score(player int) int {
	x.mu.Lock()
	defer x.mu.Unlock()
	return x.game.PointsFor(player) - outBonus(x.game)
}

func (x *Explorer) OnTurn() int {
	x.mu.Lock()
	defer x.mu.Unlock()
	return x.game.PlayerOnTurn()
}

func (x *Explorer) Over() bool {
	x.mu.Lock()
	defer x.mu.Unlock()
	return over(x.game)
}

func (x *Explorer) Ending() *EndgameLine {
	x.mu.Lock()
	defer x.mu.Unlock()
	l := &EndgameLine{}
	l.setEnding(x.game)
	return l
}

func (x *Explorer) StepCount() int {
	x.mu.Lock()
	defer x.mu.Unlock()
	return len(x.played)
}

func (x *Explorer) StepAt(i int) *Move {
	x.mu.Lock()
	defer x.mu.Unlock()
	if i < 0 || i >= len(x.played) {
		return nil
	}
	return moveOf(x.played[i])
}

func (x *Explorer) rootKeep() func(*macondo.Move) bool {
	if len(x.played) > 0 {
		return nil
	}
	return x.keep
}

func replayed(start *game.Game, moves []*macondo.Move) *game.Game {
	g := start.Copy()
	for _, m := range moves {
		if g.PlayMove(m, false, 0) != nil {
			break
		}
	}
	return g
}

func outBonus(g *game.Game) int {
	for p := range 2 {
		if over(g) && core.RackText(g, p) == "" {
			return core.RackValue(g, 1-p)
		}
	}
	return 0
}

func over(g *game.Game) bool {
	return g.Playing() == pb.PlayState_GAME_OVER
}

func moveOf(m *macondo.Move) *Move {
	return &Move{Description: describe(m), Score: m.Score()}
}
