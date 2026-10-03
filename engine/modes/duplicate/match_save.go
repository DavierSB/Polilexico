package duplicate

import (
	"encoding/json"
	"time"

	"lexico/engine/events"
	"lexico/engine/internal/core"
	"lexico/engine/internal/timing"
)

type savedMatch struct {
	Game        string
	TurnMs      int64
	InTurn      bool
	TurnSpentMs int64
}

func (m *Match) Save() (string, error) {
	m.mu.Lock()
	defer m.mu.Unlock()
	game, err := m.game.Save()
	if err != nil {
		return "", err
	}
	return core.JSON(m.saved(game))
}

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

type Progress struct {
	Round       int
	HumanTotal  int
	MasterTotal int
}

func ReadProgress(text string) (*Progress, error) {
	var m savedMatch
	if err := json.Unmarshal([]byte(text), &m); err != nil {
		return nil, err
	}
	var g savedGame
	if err := json.Unmarshal([]byte(m.Game), &g); err != nil {
		return nil, err
	}
	return &Progress{Round: len(g.Turns) + 1, HumanTotal: g.HumanTotal, MasterTotal: g.MasterTotal}, nil
}

func (m *Match) saved(game string) savedMatch {
	s := savedMatch{Game: game, TurnMs: m.turnTime.Milliseconds(), InTurn: m.phase != PhaseWaiting && m.phase != PhaseFinished}
	if usesTurnClock(m.phase) {
		s.TurnSpentMs = m.turn.Spent().Milliseconds()
	}
	return s
}

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
