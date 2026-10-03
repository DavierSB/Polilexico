package duplicate

import (
	"errors"
	"sync"
	"time"

	"lexico/engine/events"
	"lexico/engine/internal/notify"
	"lexico/engine/internal/timing"
)

const (
	PhaseWaiting     = "waiting"
	PhaseInvalidRack = "invalid_rack"
	PhaseManyInvalid = "many_invalid"
	PhasePlaying     = "playing"
	PhaseConfirming  = "confirming"
	PhaseFinished    = "finished"
)

var (
	errPaused     = errors.New("la partida está en pausa")
	errNotWaiting = errors.New("la ronda ya empezó")
	errNotPlaying = errors.New("no es momento de jugar")
)

type Match struct {
	mu       sync.Mutex
	game     *Game
	turn     *timing.Countdown
	step     *timing.Countdown
	turnTime time.Duration
	notifier *notify.Notifier
	phase    string
	draw     *Draw
	shown    int
	proposal *Attempt
	input    string
	lastErr  string
	paused   bool
	closed   bool
}

func NewMatch(turnMs int64, invalidLosesTurn bool, maxRounds int, l events.Listener) (*Match, error) {
	g, err := Start()
	if err != nil {
		return nil, err
	}
	g.SetInvalidPlayLosesTurn(invalidLosesTurn)
	g.SetMaxRounds(maxRounds)
	return startMatch(g, turnDuration(turnMs), timing.Real(), l, false), nil
}

func (m *Match) Game() *Game {
	return m.game
}

func (m *Match) ShowRack() error {
	m.mu.Lock()
	defer m.mu.Unlock()
	if err := m.check(PhaseWaiting, errNotWaiting); err != nil {
		return err
	}
	draw, err := m.game.DrawRack()
	if err != nil {
		return err
	}
	m.enterDraw(draw)
	m.afterChange()
	return nil
}

func (m *Match) Propose(input string) (*Attempt, error) {
	m.mu.Lock()
	defer m.mu.Unlock()
	if err := m.check(PhasePlaying, errNotPlaying); err != nil {
		return nil, err
	}
	attempt, err := m.game.Validate(input)
	if err != nil {
		return nil, err
	}
	m.propose(attempt, input)
	m.afterChange()
	return attempt, nil
}

func (m *Match) Cancel() {
	m.mu.Lock()
	defer m.mu.Unlock()
	if m.phase != PhaseConfirming || m.paused {
		return
	}
	m.enterPlaying(false)
	m.afterChange()
}

func (m *Match) Pause() {
	m.setPaused(true)
}

func (m *Match) Resume() {
	m.setPaused(false)
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

func startMatch(g *Game, turnTime time.Duration, clock timing.Clock, l events.Listener, paused bool) *Match {
	m := &Match{game: g, turn: timing.NewCountdown(clock), step: timing.NewCountdown(clock), turnTime: turnTime,
		notifier: notify.New(l), phase: PhaseWaiting, paused: paused}
	if g.Status().Over {
		m.phase = PhaseFinished
	}
	return m
}

func (m *Match) check(want string, wrongPhase error) error {
	switch {
	case m.paused:
		return errPaused
	case m.phase != want:
		return wrongPhase
	}
	return nil
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

func turnDuration(turnMs int64) time.Duration {
	if turnMs <= 0 {
		return TurnSeconds * time.Second
	}
	return time.Duration(turnMs) * time.Millisecond
}
