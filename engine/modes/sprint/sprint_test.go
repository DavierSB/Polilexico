package sprint

import (
	"strings"
	"testing"
	"time"
	"unicode/utf8"

	"lexico/engine/internal/core"
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

func TestMatchVoidOnlyRejectsInvalidWords(t *testing.T) {
	m, _ := newTestMatch(t)
	waitForPhase(t, m, PhaseSolving)
	if _, err := m.Propose(invalidBingo(t, m)); err == nil {
		t.Fatal("aceptó palabras no válidas")
	}
	if m.Phase() != PhaseSolving || m.Lives() != Lives {
		t.Fatalf("en void cerró la mano: fase %s, vidas %d", m.Phase(), m.Lives())
	}
}

func TestMatchSingleInvalidWordsCostALife(t *testing.T) {
	m, _ := newTestMatch(t)
	m.invalidCostsLife = true
	waitForPhase(t, m, PhaseSolving)
	if _, err := m.Propose(invalidBingo(t, m)); err == nil {
		t.Fatal("aceptó palabras no válidas")
	}
	if m.Phase() != PhaseRevealed || m.Lives() != Lives-1 || m.Outcome() != OutcomeInvalid {
		t.Fatalf("fase %s, vidas %d, resultado %s", m.Phase(), m.Lives(), m.Outcome())
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

// invalidBingo: las fichas del mejor scrabble de la mano, en las mismas casillas pero en otro
// orden (las del tablero, los ".", no se mueven), de modo que formen palabras no validas.
func invalidBingo(t *testing.T, m *Match) string {
	t.Helper()
	best := m.puzzle.solutions[0]
	tiles := tileTokens(best.Tiles)
	for shift := 1; shift < bingoTiles; shift++ {
		input := best.Coords + " " + strings.Join(rotateOwn(tiles, shift), "")
		if _, err := m.puzzle.check(input); err != nil {
			if _, invalid := core.AsInvalidWords(err); invalid {
				return input
			}
		}
	}
	t.Fatal("ningún orden de las fichas forma palabras no válidas")
	return ""
}

// rotateOwn rota `shift` puestos las fichas del atril, dejando en su sitio las del tablero (".").
func rotateOwn(tiles []string, shift int) []string {
	var own []int
	for i, tile := range tiles {
		if tile != "." {
			own = append(own, i)
		}
	}
	out := append([]string(nil), tiles...)
	for k, i := range own {
		out[i] = tiles[own[(k+shift)%len(own)]]
	}
	return out
}

// tileTokens separa "CA[CH]O" en fichas: los digrafos entre corchetes cuentan como una.
func tileTokens(s string) []string {
	var out []string
	for i := 0; i < len(s); {
		j := i + 1
		if s[i] == '[' {
			j = strings.IndexByte(s[i:], ']') + i + 1
		} else {
			for j < len(s) && !utf8.RuneStart(s[j]) {
				j++
			}
		}
		out = append(out, s[i:j])
		i = j
	}
	return out
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
