package sprint

import (
	"testing"
	"time"

	"lexico/engine/events"
)

func TestMatchCuesASolvedHand(t *testing.T) {
	m, _ := newTestMatch(t)
	waitForPhase(t, m, PhaseSolving)
	best := m.puzzle.solutions[0]
	if _, err := m.Propose(best.Coords + " " + best.Tiles); err != nil {
		t.Fatal(err)
	}
	if m.CueCount() != 1 || m.Cue() != events.CueCorrect {
		t.Fatalf("%d avisos, el último %q", m.CueCount(), m.Cue())
	}
}

func TestMatchCuesInvalidWordsEvenWithoutLosingALife(t *testing.T) {
	m, _ := newTestMatch(t)
	waitForPhase(t, m, PhaseSolving)
	m.Propose(invalidBingo(t, m))
	if m.CueCount() != 1 || m.Cue() != events.CueWrong {
		t.Fatalf("%d avisos, el último %q", m.CueCount(), m.Cue())
	}
}

func TestMatchCuesTheLastSecondsAndTheEnd(t *testing.T) {
	m, clock := newTestMatch(t)
	waitForPhase(t, m, PhaseSolving)
	clock.Advance((MatchSeconds - 30) * time.Second)
	if m.Cue() != events.CueWarning {
		t.Fatalf("a falta de 30 s: %q", m.Cue())
	}
	clock.Advance(30 * time.Second)
	if m.Phase() != PhaseFinished || m.CueCount() != 3 || m.Cue() != events.CueTimeUp {
		t.Fatalf("fase %s, %d avisos, el último %q", m.Phase(), m.CueCount(), m.Cue())
	}
}

func TestMatchCuesGivingUpAsAMistake(t *testing.T) {
	m, _ := newTestMatch(t)
	waitForPhase(t, m, PhaseSolving)
	if err := m.GiveUp(); err != nil {
		t.Fatal(err)
	}
	if m.CueCount() != 1 || m.Cue() != events.CueWrong {
		t.Fatalf("%d avisos, el último %q", m.CueCount(), m.Cue())
	}
}
