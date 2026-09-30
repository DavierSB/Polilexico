package classic

import (
	"errors"
	"sync"

	"lexico/engine/events"
	"lexico/engine/internal/notify"
	"lexico/engine/internal/timing"
)

var errPaused = errors.New("la partida está en pausa")

type Match struct {
	mu       sync.Mutex
	game     *Game
	clocks   *clockPair
	timeout  *timing.Alarm
	notifier *notify.Notifier
	paused   bool
	closed   bool
	thinking bool
	botError string
}

func NewMatch(botName string, timeMs, overtimeMs int64, invalidLosesTurn bool, l events.Listener) (*Match, error) {
	g, err := Start(botName)
	if err != nil {
		return nil, err
	}
	return newMatch(g, timeMs, overtimeMs, invalidLosesTurn, l), nil
}

func (m *Match) Game() *Game {
	return m.game
}

func (m *Match) Play(input string) (*Move, error) {
	m.mu.Lock()
	defer m.mu.Unlock()
	if m.paused {
		return nil, errPaused
	}
	played, err := m.game.Play(input)
	if err == nil {
		m.afterChange()
	}
	return played, err
}

func (m *Match) Pause() {
	m.setPaused(true)
}

func (m *Match) Resume() {
	m.setPaused(false)
}

func (m *Match) Paused() bool {
	m.mu.Lock()
	defer m.mu.Unlock()
	return m.paused
}

func (m *Match) BotError() string {
	m.mu.Lock()
	defer m.mu.Unlock()
	return m.botError
}

func (m *Match) Close() {
	m.mu.Lock()
	defer m.mu.Unlock()
	if m.closed {
		return
	}
	m.closed = true
	m.afterChange()
	m.notifier.Close()
}

func newMatch(g *Game, timeMs, overtimeMs int64, invalidLosesTurn bool, l events.Listener) *Match {
	g.SetInvalidPlayLosesTurn(invalidLosesTurn)
	clock := timing.Real()
	return startMatch(g, newClockPair(clock, timeMs, overtimeMs, 0, 0), clock, l, false)
}

func startMatch(g *Game, clocks *clockPair, clock timing.Clock, l events.Listener, paused bool) *Match {
	m := &Match{game: g, clocks: clocks, timeout: timing.NewAlarm(clock), notifier: notify.New(l), paused: paused}
	m.mu.Lock()
	defer m.mu.Unlock()
	m.afterChange()
	return m
}

func (m *Match) setPaused(paused bool) {
	m.mu.Lock()
	defer m.mu.Unlock()
	if m.closed || m.paused == paused {
		return
	}
	m.paused = paused
	m.afterChange()
}
