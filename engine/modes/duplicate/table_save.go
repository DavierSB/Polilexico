package duplicate

import (
	"encoding/json"
	"time"

	"lexico/engine/internal/core"
	"lexico/engine/events"
	"lexico/engine/internal/timing"
)

type savedTable struct {
	Game    string
	Players []string
	Rounds  []*tableRound
	Totals  map[string]int
	TurnMs  int64
	Live    bool
}

func (t *Table) Save() (string, error) {
	t.mu.Lock()
	defer t.mu.Unlock()
	game, err := t.game.Save()
	if err != nil {
		return "", err
	}
	return core.JSON(savedTable{Game: game, Players: t.players, Rounds: t.rounds, Totals: t.totals, TurnMs: t.turnTime.Milliseconds(), Live: t.live})
}

func LoadTable(text string, l events.Listener) (*Table, error) {
	var s savedTable
	if err := json.Unmarshal([]byte(text), &s); err != nil {
		return nil, err
	}
	g, err := Load(s.Game)
	if err != nil {
		return nil, err
	}
	g.SetInvalidPlayLosesTurn(true)
	t := startTable(g, s.Players, time.Duration(s.TurnMs)*time.Millisecond, timing.Real(), s.Live, l)
	t.restore(s)
	return t, nil
}

func (t *Table) restore(s savedTable) {
	t.mu.Lock()
	defer t.mu.Unlock()
	t.rounds = s.Rounds
	for name, total := range s.Totals {
		t.totals[name] = total
	}
}
