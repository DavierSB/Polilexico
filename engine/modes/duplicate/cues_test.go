package duplicate

import (
	"testing"
	"time"

	"lexico/engine/events"
	"lexico/engine/internal/timing"
)

func TestMatchCuesTheTurnClock(t *testing.T) {
	m, clock := newTestMatch(t)
	mustShowRack(t, m, clock)
	steps := []struct {
		advance time.Duration
		cue     string
	}{
		{(TurnSeconds - 30) * time.Second, events.CueWarning},
		{25 * time.Second, events.CueLastSeconds},
		{5 * time.Second, events.CueMiss},
	}
	for i, s := range steps {
		clock.Advance(s.advance)
		if m.CueCount() != i+1 || m.Cue() != s.cue {
			t.Fatalf("paso %d: %d avisos, el último %q", i, m.CueCount(), m.Cue())
		}
	}
}

func TestMatchPausedTurnStaysQuiet(t *testing.T) {
	m, clock := newTestMatch(t)
	mustShowRack(t, m, clock)
	m.Pause()
	clock.Advance(time.Hour)
	if m.CueCount() != 0 {
		t.Fatalf("avisó en pausa: %q", m.Cue())
	}
}

func TestMatchCuesTheEndOfTheGame(t *testing.T) {
	g := newTestGame(t)
	g.SetMaxRounds(1)
	clock := timing.NewFake()
	m := startMatch(g, TurnSeconds*time.Second, clock, nil, false)
	t.Cleanup(m.Close)
	mustShowRack(t, m, clock)
	mustPropose(t, m, "pasar")
	if m.Phase() != PhaseFinished || m.Cue() != events.CueGameOver {
		t.Fatalf("fase %s, aviso %q", m.Phase(), m.Cue())
	}
}

func TestMatchCuesAHit(t *testing.T) {
	m, clock := newTestMatch(t)
	mustShowRack(t, m, clock)
	mustPropose(t, m, masterInput(m.game))
	clock.Advance(CancelSeconds * time.Second)
	if !m.game.TurnAt(0).Hit || m.CueCount() != 1 || m.Cue() != events.CueCorrect {
		t.Fatalf("acierto %v, %d avisos, el último %q", m.game.TurnAt(0).Hit, m.CueCount(), m.Cue())
	}
}
