package sprint

import (
	"testing"
	"time"

	"lexico/engine/internal/testenv"
	"lexico/engine/internal/timing"
)

// Cuanto se espera, como mucho, a que HastyBot encuentre un problema.
const searchTimeout = 2 * time.Minute

func TestMatchPosesAPuzzleWithBingos(t *testing.T) {
	m, _ := newTestMatch(t)
	waitForPhase(t, m, PhaseSolving)
	if m.Board() == "" || m.Rack() == "" || m.SolutionCount() == 0 || m.RemainingMs() != MatchSeconds*1000 {
		t.Fatalf("mano incompleta: atril %q, %d scrabbles, quedan %d", m.Rack(), m.SolutionCount(), m.RemainingMs())
	}
	if m.SolutionAt(0) != nil {
		t.Fatal("las soluciones se ven antes de cerrar la mano")
	}
}

func TestMatchAcceptsABingo(t *testing.T) {
	m, _ := newTestMatch(t)
	waitForPhase(t, m, PhaseSolving)
	best := m.puzzle.solutions[0]
	if _, err := m.Propose(best.Coords + " " + best.Tiles); err != nil {
		t.Fatal(err)
	}
	if m.Phase() != PhaseRevealed || m.Solved() != 1 || m.Lives() != Lives || m.Outcome() != OutcomeSolved {
		t.Fatalf("fase %s, resueltas %d, vidas %d", m.Phase(), m.Solved(), m.Lives())
	}
}

func TestMatchRejectsPlaysThatAreNotBingos(t *testing.T) {
	m, _ := newTestMatch(t)
	waitForPhase(t, m, PhaseSolving)
	if _, err := m.Propose("pasar"); err != errNotBingo {
		t.Fatalf("aceptó un pase: %v", err)
	}
	if m.Phase() != PhaseSolving || m.Lives() != Lives {
		t.Fatalf("un intento fallido cerró la mano: fase %s, vidas %d", m.Phase(), m.Lives())
	}
}

func TestMatchEndsWhenTheClockRunsOut(t *testing.T) {
	m, clock := newTestMatch(t)
	waitForPhase(t, m, PhaseSolving)
	clock.Advance(MatchSeconds * time.Second)
	if m.Phase() != PhaseFinished || m.Lives() != Lives || m.Outcome() != OutcomeTimeout || m.SolutionAt(0) == nil {
		t.Fatalf("fase %s, vidas %d, resultado %s", m.Phase(), m.Lives(), m.Outcome())
	}
}

func TestMatchClockOnlyRunsWhileSolving(t *testing.T) {
	m, clock := newTestMatch(t)
	waitForPhase(t, m, PhaseSolving)
	clock.Advance(20 * time.Second)
	best := m.puzzle.solutions[0]
	if _, err := m.Propose(best.Coords + " " + best.Tiles); err != nil {
		t.Fatal(err)
	}
	clock.Advance(time.Hour)
	m.Next()
	waitForPhase(t, m, PhaseSolving)
	if left := m.RemainingMs(); left != (MatchSeconds-20)*1000 {
		t.Fatalf("el reloj corrió entre manos: quedan %d", left)
	}
}

func TestMatchPauseStopsTheClock(t *testing.T) {
	m, clock := newTestMatch(t)
	waitForPhase(t, m, PhaseSolving)
	clock.Advance(20 * time.Second)
	m.Pause()
	clock.Advance(time.Hour)
	if m.Phase() != PhaseSolving || m.RemainingMs() != (MatchSeconds-20)*1000 {
		t.Fatalf("fase %s, quedan %d", m.Phase(), m.RemainingMs())
	}
	if err := m.GiveUp(); err != errPaused {
		t.Fatalf("en pausa aceptó acciones: %v", err)
	}
}

func TestMatchEndsWithoutLives(t *testing.T) {
	m, _ := newTestMatch(t)
	if m.MaxLives() != Lives {
		t.Fatalf("empezó con %d vidas", m.MaxLives())
	}
	for i := 0; i < Lives; i++ {
		waitForPhase(t, m, PhaseSolving)
		if err := m.GiveUp(); err != nil {
			t.Fatal(err)
		}
		m.Next()
	}
	if m.Phase() != PhaseFinished || m.Lives() != 0 || m.Posed() != Lives {
		t.Fatalf("fase %s, vidas %d, manos %d", m.Phase(), m.Lives(), m.Posed())
	}
}

// newTestMatch: una serie con reloj falso que se para en el primer turno con scrabble posible.
func newTestMatch(t *testing.T) (*Match, *timing.Fake) {
	t.Helper()
	testenv.Init(t)
	clock := timing.NewFake()
	m := startMatch(MatchSeconds*time.Second, Lives, clock, func() int { return 1 }, nil)
	t.Cleanup(m.Close)
	return m, clock
}

func TestStartingLivesStayInRange(t *testing.T) {
	for lives, want := range map[int]int{0: Lives, 1: 1, MaxLives: MaxLives, MaxLives + 1: Lives} {
		if got := startingLives(lives); got != want {
			t.Errorf("startingLives(%d) = %d, quería %d", lives, got, want)
		}
	}
}

// waitForPhase espera a que la serie llegue a `phase` (la busqueda corre en otro hilo).
func waitForPhase(t *testing.T, m *Match, phase string) {
	t.Helper()
	deadline := time.Now().Add(searchTimeout)
	for m.Phase() != phase {
		if time.Now().After(deadline) {
			t.Fatalf("la serie sigue en %s (%s)", m.Phase(), m.LastError())
		}
		time.Sleep(10 * time.Millisecond)
	}
}
