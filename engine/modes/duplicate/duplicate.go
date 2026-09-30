package duplicate

import (
	"sync"
	"time"

	"github.com/domino14/macondo/game"
	"github.com/domino14/macondo/move"
	"github.com/domino14/macondo/movegen"

	"lexico/engine/internal/core"
)

const (
	TurnSeconds        = 200
	CancelSeconds      = 10
	InvalidRackSeconds = 5
)

const masterIdx = 0

type Game struct {
	mu                            sync.Mutex
	g                             *game.Game
	master                        *movegen.GordonGenerator
	turn                          int
	rack                          string
	boardBefore                   string
	plays                         []*move.Move
	masterTotal, humanTotal, hits int
	turns                         []*Turn
	log                           *gameLog
	ended                         bool
	invalidLosesTurn              bool
	recordPath                    string
}

func Start() (*Game, error) {
	g, err := core.NewGame("Máster", "Máster 2")
	if err != nil {
		return nil, err
	}
	g.StartGame()
	return newGame(g, &gameLog{StartedAt: time.Now()})
}

func (d *Game) SetInvalidPlayLosesTurn(on bool) {
	d.mu.Lock()
	defer d.mu.Unlock()
	d.invalidLosesTurn = on
}

func newGame(g *game.Game, log *gameLog) (*Game, error) {
	seatMaster(g)
	master, err := core.NewMaster(g)
	if err != nil {
		return nil, err
	}
	return &Game{g: g, master: master, log: log}, nil
}

func seatMaster(g *game.Game) {
	g.ThrowRacksInFor(1 - masterIdx)
	g.SetPlayerOnTurn(masterIdx)
}
