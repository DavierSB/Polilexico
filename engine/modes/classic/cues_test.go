package classic

import (
	"testing"
	"time"

	"lexico/engine/events"
)

func TestMatchCuesYourClock(t *testing.T) {
	m, clock, _ := newTestMatch(t, 120_000, false)
	steps := []struct {
		advance time.Duration
		cue     string
	}{
		{time.Minute, events.CueWarning},
		{time.Minute, events.CueTimeUp},
		{30 * time.Second, events.CueTimeUp},
	}
	for i, s := range steps {
		clock.Advance(s.advance)
		if m.CueCount() != i+1 || m.Cue() != s.cue {
			t.Fatalf("paso %d: %d avisos, el último %q", i, m.CueCount(), m.Cue())
		}
	}
	if r := m.Game().Result(); r == nil || !r.LostOnTime {
		t.Fatalf("no perdió por tiempo: %+v", r)
	}
}

func TestMatchCuesTheBotMove(t *testing.T) {
	m, _, l := newTestMatch(t, 0, false)
	playYourTurn(t, m)
	l.waitFor(t, func() bool { return m.Game().Status().HumanToMove })
	if m.CueCount() != 1 || m.Cue() != events.CueOpponent {
		t.Fatalf("%d avisos, el último %q", m.CueCount(), m.Cue())
	}
}
