package duplicate

import (
	"encoding/json"
	"errors"

	"lexico/engine/internal/core"
)

var errNoLog = errors.New("partida guardada sin registro")

type savedGame struct {
	Turn             int
	MasterTotal      int
	HumanTotal       int
	Hits             int
	Turns            []*Turn
	Log              *gameLog
	Ended            bool
	InvalidLosesTurn bool
	MaxRounds        int
	RecordPath       string
	Rack             string
	History          json.RawMessage
}

func (d *Game) Save() (string, error) {
	d.mu.Lock()
	defer d.mu.Unlock()
	history, err := core.HistoryJSON(d.g)
	if err != nil {
		return "", err
	}
	return core.JSON(d.saved(history))
}

func Load(text string) (*Game, error) {
	var s savedGame
	if err := json.Unmarshal([]byte(text), &s); err != nil {
		return nil, err
	}
	if s.Log == nil {
		return nil, errNoLog
	}
	return s.resume()
}

func (d *Game) saved(history json.RawMessage) savedGame {
	return savedGame{Turn: d.savedTurn(), MasterTotal: d.masterTotal, HumanTotal: d.humanTotal,
		Hits: d.hits, Turns: d.turns, Log: d.log, Ended: d.ended, InvalidLosesTurn: d.invalidLosesTurn,
		MaxRounds: d.maxRounds, RecordPath: d.recordPath, Rack: d.g.RackLettersFor(masterIdx), History: history}
}

func (d *Game) savedTurn() int {
	if d.inTurn() {
		return d.turn - 1
	}
	return d.turn
}

func (s savedGame) resume() (*Game, error) {
	g, err := core.GameFromHistory(s.History, []string{s.Rack, ""})
	if err != nil {
		return nil, err
	}
	d, err := newGame(g, s.Log)
	if err != nil {
		return nil, err
	}
	d.restore(s)
	return d, nil
}

func (d *Game) restore(s savedGame) {
	d.turn = s.Turn
	d.masterTotal, d.humanTotal, d.hits = s.MasterTotal, s.HumanTotal, s.Hits
	d.turns = s.Turns
	d.ended = s.Ended
	d.invalidLosesTurn = s.InvalidLosesTurn
	d.maxRounds = s.MaxRounds
	d.recordPath = s.RecordPath
}
