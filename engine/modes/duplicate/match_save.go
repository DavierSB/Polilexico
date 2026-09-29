package duplicate

import (
	"encoding/json"
	"time"

	"lexico/engine/events"
	"lexico/engine/internal/core"
	"lexico/engine/internal/timing"
)

// savedMatch es una duplicada en marcha guardada: la partida, el tiempo por turno y, a mitad de
// ronda, lo gastado del turno (la jugada propuesta no se guarda: al continuar se vuelve a pensar).
type savedMatch struct {
	Game        string
	TurnMs      int64
	InTurn      bool
	TurnSpentMs int64
}

// Save devuelve la partida en marcha como texto, para continuarla con LoadMatch.
func (m *Match) Save() (string, error) {
	m.mu.Lock()
	defer m.mu.Unlock()
	game, err := m.game.Save()
	if err != nil {
		return "", err
	}
	return core.JSON(m.saved(game))
}

// LoadMatch continua una duplicada guardada con Match.Save. Empieza en pausa; a mitad de ronda,
// con el mismo atril y lo que quedaba del turno.
func LoadMatch(text string, l events.Listener) (*Match, error) {
	var s savedMatch
	if err := json.Unmarshal([]byte(text), &s); err != nil {
		return nil, err
	}
	g, err := Load(s.Game)
	if err != nil {
		return nil, err
	}
	m := startMatch(g, turnDuration(s.TurnMs), timing.Real(), l, true)
	return m, m.resumeTurn(s)
}

func (m *Match) saved(game string) savedMatch {
	s := savedMatch{Game: game, TurnMs: m.turnTime.Milliseconds(), InTurn: m.phase != PhaseWaiting && m.phase != PhaseFinished}
	if usesTurnClock(m.phase) {
		s.TurnSpentMs = m.turn.Spent().Milliseconds()
	}
	return s
}

// resumeTurn vuelve a la ronda guardada a medias: el mismo atril y lo gastado del turno.
func (m *Match) resumeTurn(s savedMatch) error {
	m.mu.Lock()
	defer m.mu.Unlock()
	if s.InTurn && m.phase == PhaseWaiting {
		draw, err := m.game.DrawRack()
		if err != nil {
			return err
		}
		m.draw, m.phase = draw, PhasePlaying
		m.turn.Resume(m.turnTime, time.Duration(s.TurnSpentMs)*time.Millisecond)
	}
	m.afterChange()
	return nil
}
