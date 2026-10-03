package sprint

import (
	"errors"
	"math/rand/v2"
	"sync"
	"time"

	"lexico/engine/events"
	"lexico/engine/internal/core"
	"lexico/engine/internal/notify"
	"lexico/engine/internal/timing"
)

const (
	Lives        = 3
	MaxLives     = 5
	MatchSeconds = 300
	MaxTarget    = 20
)

const (
	PhaseSearching = "searching"
	PhaseSolving   = "solving"
	PhaseRevealed  = "revealed"
	PhaseFinished  = "finished"
)

const (
	OutcomeSolved  = "solved"
	OutcomeTimeout = "timeout"
	OutcomeGaveUp  = "gave_up"
	OutcomeInvalid = "invalid"
)

var (
	errPaused     = errors.New("la partida está en pausa")
	errNotSolving = errors.New("no hay ninguna mano en juego")
)

type Match struct {
	mu                   sync.Mutex
	hunter               *hunter
	clock                *timing.Countdown
	totalTime            time.Duration
	maxLives             int
	invalidCostsLife     bool
	notifier             *notify.Notifier
	phase                string
	puzzle               *puzzle
	lives, solved, posed int
	outcome              string
	answer               *Solution
	lastErr              string
	paused, closed       bool
}

func NewMatch(totalMs int64, lives int, invalidCostsLife bool, difficulty string, l events.Listener) (*Match, error) {
	if err := core.Ready(); err != nil {
		return nil, err
	}
	if !validDifficulty(difficulty) {
		return nil, errDifficulty
	}
	m := startMatch(totalDuration(totalMs), startingLives(lives), timing.Real(), randomTarget, difficulty, l)
	m.invalidCostsLife = invalidCostsLife
	return m, nil
}

func (m *Match) Propose(input string) (*Solution, error) {
	m.mu.Lock()
	defer m.mu.Unlock()
	if err := m.checkSolving(); err != nil {
		return nil, err
	}
	s, err := m.puzzle.check(input)
	if _, invalid := core.AsInvalidWords(err); invalid && m.invalidCostsLife {
		m.reveal(OutcomeInvalid, nil)
		m.afterChange()
		return nil, err
	}
	if err != nil {
		return nil, err
	}
	m.reveal(OutcomeSolved, &s)
	m.afterChange()
	return &s, nil
}

func (m *Match) GiveUp() error {
	m.mu.Lock()
	defer m.mu.Unlock()
	if err := m.checkSolving(); err != nil {
		return err
	}
	m.reveal(OutcomeGaveUp, nil)
	m.afterChange()
	return nil
}

func (m *Match) Next() {
	m.mu.Lock()
	defer m.mu.Unlock()
	if m.phase != PhaseRevealed || m.paused {
		return
	}
	m.search()
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
	m.hunter.stop()
	m.afterChange()
	m.notifier.Close()
}

func startMatch(total time.Duration, lives int, clock timing.Clock, pick func() int, difficulty string, l events.Listener) *Match {
	m := &Match{hunter: startHunter(pick, difficulty), clock: timing.NewCountdown(clock), totalTime: total,
		maxLives: lives, notifier: notify.New(l), lives: lives}
	m.mu.Lock()
	defer m.mu.Unlock()
	m.clock.Restart(total)
	m.search()
	m.afterChange()
	return m
}

func (m *Match) checkSolving() error {
	switch {
	case m.paused:
		return errPaused
	case m.phase != PhaseSolving:
		return errNotSolving
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

func randomTarget() int {
	return rand.IntN(MaxTarget) + 1
}

func totalDuration(totalMs int64) time.Duration {
	if totalMs <= 0 {
		return MatchSeconds * time.Second
	}
	return time.Duration(totalMs) * time.Millisecond
}

func startingLives(lives int) int {
	if lives < 1 || lives > MaxLives {
		return Lives
	}
	return lives
}
