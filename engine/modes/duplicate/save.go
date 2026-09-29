package duplicate

import (
	"encoding/json"
	"errors"

	"lexico/engine/internal/core"
)

var errNoLog = errors.New("partida guardada sin registro")

// savedGame es una partida guardada: el historial de macondo mas lo que lleva este paquete.
type savedGame struct {
	Turn             int
	MasterTotal      int
	HumanTotal       int
	Hits             int
	Turns            []*Turn
	Log              *gameLog
	Ended            bool
	InvalidLosesTurn bool
	RecordPath       string
	Rack             string // el atril del máster (a mitad de turno, el que ya se vio)
	History          json.RawMessage
}

// Save devuelve la partida como texto, para continuarla despues con Load. A mitad de turno,
// al continuar DrawRack vuelve a dar el mismo atril.
func (d *Game) Save() (string, error) {
	d.mu.Lock()
	defer d.mu.Unlock()
	history, err := core.HistoryJSON(d.g)
	if err != nil {
		return "", err
	}
	return core.JSON(d.saved(history))
}

// Load continua una partida guardada con Save.
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
		RecordPath: d.recordPath, Rack: d.g.RackLettersFor(masterIdx), History: history}
}

// A mitad de turno se guarda como si no se hubiera visto el atril: DrawRack lo repetira.
func (d *Game) savedTurn() int {
	if d.inTurn() {
		return d.turn - 1
	}
	return d.turn
}

// resume rehace la partida de macondo, con el atril que ya se habia visto, y le devuelve lo
// que lleva este paquete.
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
	d.recordPath = s.RecordPath
}
