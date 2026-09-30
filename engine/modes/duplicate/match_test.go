package duplicate

import (
	"testing"
	"time"

	"lexico/engine/internal/core"
	"lexico/engine/internal/timing"
)

func TestMatchRoundIsConfirmedAfterTheCancelWindow(t *testing.T) {
	m, clock := newTestMatch(t)
	mustShowRack(t, m, clock)
	mustPropose(t, m, masterInput(m.game))
	if m.Phase() != PhaseConfirming || m.CancelRemainingMs() != CancelSeconds*1000 {
		t.Fatalf("fase %s, cancelar %d", m.Phase(), m.CancelRemainingMs())
	}
	clock.Advance(CancelSeconds * time.Second)
	if m.Phase() != PhaseWaiting || !m.game.TurnAt(0).Hit {
		t.Fatalf("fase %s, turno %+v", m.Phase(), m.game.TurnAt(0))
	}
}

func TestMatchCancelGoesBackToPlaying(t *testing.T) {
	m, clock := newTestMatch(t)
	mustShowRack(t, m, clock)
	mustPropose(t, m, masterInput(m.game))
	clock.Advance(3 * time.Second)
	m.Cancel()
	clock.Advance(CancelSeconds * time.Second)
	if m.Phase() != PhasePlaying || m.game.TurnCount() != 0 {
		t.Fatalf("fase %s, turnos %d", m.Phase(), m.game.TurnCount())
	}
}

func TestMatchTurnTimesOut(t *testing.T) {
	m, clock := newTestMatch(t)
	mustShowRack(t, m, clock)
	clock.Advance(TurnSeconds * time.Second)
	if m.Phase() != PhaseWaiting || m.game.TurnAt(0).HumanKind != core.KindTimeout {
		t.Fatalf("fase %s, turno %+v", m.Phase(), m.game.TurnAt(0))
	}
}

func TestMatchPauseStopsTheTurnClock(t *testing.T) {
	m, clock := newTestMatch(t)
	mustShowRack(t, m, clock)
	clock.Advance(50 * time.Second)
	m.Pause()
	clock.Advance(time.Hour)
	if m.Phase() != PhasePlaying || m.TurnRemainingMs() != 150_000 {
		t.Fatalf("fase %s, quedan %d", m.Phase(), m.TurnRemainingMs())
	}
	if _, err := m.Propose("pasar"); err != errPaused {
		t.Fatalf("en pausa aceptó acciones: %v", err)
	}
}

func TestMatchSaveMidTurnKeepsRackAndTime(t *testing.T) {
	m, clock := newTestMatch(t)
	mustShowRack(t, m, clock)
	clock.Advance(30 * time.Second)
	loaded := mustReloadMatch(t, m)
	if !loaded.Paused() || loaded.Phase() != PhasePlaying || loaded.Rack() != m.Rack() || loaded.TurnRemainingMs() != 170_000 {
		t.Fatalf("pausa %v, fase %s, atril %q/%q, quedan %d", loaded.Paused(), loaded.Phase(), loaded.Rack(), m.Rack(), loaded.TurnRemainingMs())
	}
}

func mustReloadMatch(t *testing.T, m *Match) *Match {
	t.Helper()
	text, err := m.Save()
	if err != nil {
		t.Fatal(err)
	}
	loaded, err := LoadMatch(text, nil)
	if err != nil {
		t.Fatal(err)
	}
	t.Cleanup(loaded.Close)
	return loaded
}

func newTestMatch(t *testing.T) (*Match, *timing.Fake) {
	t.Helper()
	clock := timing.NewFake()
	m := startMatch(newTestGame(t), TurnSeconds*time.Second, clock, nil, false)
	t.Cleanup(m.Close)
	return m, clock
}

func mustShowRack(t *testing.T, m *Match, clock *timing.Fake) {
	t.Helper()
	if err := m.ShowRack(); err != nil {
		t.Fatal(err)
	}
	if m.Phase() == PhaseInvalidRack {
		clock.Advance(InvalidRackSeconds * time.Second)
	}
	if m.Phase() != PhasePlaying {
		t.Fatalf("fase %s", m.Phase())
	}
}

func mustPropose(t *testing.T, m *Match, input string) {
	t.Helper()
	if _, err := m.Propose(input); err != nil {
		t.Fatal(err)
	}
}
