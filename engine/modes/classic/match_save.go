package classic

import (
	"encoding/json"

	"lexico/engine/events"
	"lexico/engine/internal/core"
	"lexico/engine/internal/timing"
)

// savedMatch es una partida en marcha guardada: la partida mas sus relojes.
type savedMatch struct {
	Game         string
	TimeMs       int64
	OvertimeMs   int64
	HumanSpentMs int64
	BotSpentMs   int64
	// HideUnseen: ver Match.SetShowUnseen. Falta en las guardadas antes de la opcion (= mostrarlas).
	HideUnseen bool `json:",omitempty"`
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

// LoadMatch continua una partida guardada con Match.Save. Empieza en pausa.
func LoadMatch(text string, l events.Listener) (*Match, error) {
	var s savedMatch
	if err := json.Unmarshal([]byte(text), &s); err != nil {
		return nil, err
	}
	g, err := Load(s.Game)
	if err != nil {
		return nil, err
	}
	clock := timing.Real()
	m := startMatch(g, s.clockPair(clock), clock, l, true)
	m.hideUnseen = s.HideUnseen
	return m, nil
}

func (m *Match) saved(game string) savedMatch {
	s := savedMatch{Game: game, HideUnseen: m.hideUnseen}
	if p := m.clocks; p != nil {
		s.TimeMs, s.OvertimeMs = p.time.Milliseconds(), p.overtime.Milliseconds()
		s.HumanSpentMs, s.BotSpentMs = p.human.Spent().Milliseconds(), p.bot.Spent().Milliseconds()
	}
	return s
}

func (s savedMatch) clockPair(clock timing.Clock) *clockPair {
	return newClockPair(clock, s.TimeMs, s.OvertimeMs, s.HumanSpentMs, s.BotSpentMs)
}
