package classic

import (
	"encoding/json"

	"lexico/engine/internal/core"
)

// savedGame es una partida guardada: el historial de macondo mas lo que lleva este paquete.
type savedGame struct {
	HumanIdx         int
	BotName          string
	Log              *gameLog
	Moves            []*Move
	LostOnTime       bool
	InvalidLosesTurn bool
	RecordPath       string
	History          json.RawMessage
}

// Save devuelve la partida como texto, para continuarla despues con Load.
func (c *Game) Save() (string, error) {
	c.mu.Lock()
	defer c.mu.Unlock()
	history, err := core.HistoryJSON(c.g)
	if err != nil {
		return "", err
	}
	return core.JSON(c.saved(history))
}

// Load continua una partida guardada con Save.
func Load(text string) (*Game, error) {
	var s savedGame
	if err := json.Unmarshal([]byte(text), &s); err != nil {
		return nil, err
	}
	return s.resume()
}

func (c *Game) saved(history json.RawMessage) savedGame {
	return savedGame{HumanIdx: c.humanIdx, BotName: c.botName, Log: c.log, Moves: c.moves,
		LostOnTime: c.lostOnTime, InvalidLosesTurn: c.invalidLosesTurn, RecordPath: c.recordPath,
		History: history}
}

// resume rehace la partida de macondo y le devuelve lo que lleva este paquete.
func (s savedGame) resume() (*Game, error) {
	g, err := core.GameFromHistory(s.History, nil)
	if err != nil {
		return nil, err
	}
	c, err := newGame(g, s.BotName, s.HumanIdx, s.Log)
	if err != nil {
		return nil, err
	}
	c.restore(s)
	return c, nil
}

func (c *Game) restore(s savedGame) {
	c.moves = s.Moves
	c.lostOnTime = s.LostOnTime
	c.invalidLosesTurn = s.InvalidLosesTurn
	c.recordPath = s.RecordPath
}
