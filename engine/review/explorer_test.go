package review

import (
	"strings"
	"sync/atomic"
	"testing"
)

type counter struct {
	calls atomic.Int32
}

func (c *counter) OnChange() {
	c.calls.Add(1)
}

func TestExplorerPlaysTheBestMoveAndGoesBack(t *testing.T) {
	x := explorer(t, playedEndgame(t))
	board, mover := x.Board(), x.OnTurn()
	best := run(t, analysis(t, x), &counter{}).LineAt(0)
	if _, err := x.Play(input(best.Description)); err != nil {
		t.Fatalf("%s: %v", best.Description, err)
	}
	if x.StepCount() != 1 || x.OnTurn() == mover && !x.Over() {
		t.Fatalf("la jugada no cambió el turno: %d pasos", x.StepCount())
	}
	if err := x.Back(); err != nil || x.Board() != board || x.OnTurn() != mover {
		t.Fatalf("deshacer no devolvió la posición: %v", err)
	}
}

func TestExplorerScoresTheEndingWithLexicoRules(t *testing.T) {
	x := explorer(t, playedEndgame(t))
	var before [2]int
	var mover, points int
	for !x.Over() {
		best := run(t, analysis(t, x), &counter{}).LineAt(0)
		before, mover = [2]int{x.Score(0), x.Score(1)}, x.OnTurn()
		played, err := x.Play(input(best.Description))
		if err != nil {
			t.Fatal(err)
		}
		points = played.Score
	}
	e := x.Ending()
	for p, delta := range [2]int{e.MoverDelta, e.OtherDelta} {
		want := before[p] + delta
		if p == mover {
			want += points
		}
		if x.Score(p) != want {
			t.Fatalf("jugador %d: %d puntos, se esperaban %d (%+v)", p, x.Score(p), want, *e)
		}
	}
}

func TestExplorerRejectsInvalidMoves(t *testing.T) {
	x := explorer(t, playedEndgame(t))
	if _, err := x.Play("h8 ZZZZZZZ"); err == nil {
		t.Fatal("aceptó una jugada imposible")
	}
	if x.Back() == nil {
		t.Fatal("deshizo sin jugadas")
	}
}

func TestAnalysisPublishesEachCandidate(t *testing.T) {
	c := &counter{}
	e := run(t, analysis(t, explorer(t, playedEndgame(t))), c)
	if int(c.calls.Load()) <= e.LineCount() {
		t.Fatalf("solo %d avisos para %d líneas", c.calls.Load(), e.LineCount())
	}
}

func TestStoppedAnalysisLeavesPendingLines(t *testing.T) {
	a := analysis(t, explorer(t, playedEndgame(t)))
	a.Stop()
	e := run(t, a, &counter{})
	if e.LineCount() == 0 || e.LineAt(e.LineCount()-1).Depth != 0 {
		t.Fatalf("el análisis detenido siguió: %+v", e)
	}
}

func explorer(t *testing.T, g *Game) *Explorer {
	t.Helper()
	x, err := g.TurnAt(g.StartTurn).Explore()
	if err != nil {
		t.Fatal(err)
	}
	return x
}

func analysis(t *testing.T, x *Explorer) *Analysis {
	t.Helper()
	a, err := x.Analysis(analysisBudgetMs)
	if err != nil || a == nil {
		t.Fatalf("sin análisis: %v", err)
	}
	return a
}

func run(t *testing.T, a *Analysis, c *counter) *Endgame {
	t.Helper()
	if err := a.Run(c); err != nil {
		t.Fatal(err)
	}
	return a.Latest()
}

func input(description string) string {
	if strings.HasPrefix(description, "(") {
		return "pasar"
	}
	return strings.Join(strings.Fields(description)[:2], " ")
}
