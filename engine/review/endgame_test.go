package review

import (
	"testing"

	"lexico/engine/internal/testenv"
	"lexico/engine/modes/classic"
)

const analysisBudgetMs = 500

func TestEndgameTurnsAreTheOnesWithAnEmptyBag(t *testing.T) {
	g := playedEndgame(t)
	if g.TurnAt(0).Endgame {
		t.Fatal("el primer turno no puede ser del final")
	}
	if !g.TurnAt(g.StartTurn).Endgame {
		t.Fatalf("el turno %d empieza con la bolsa vacía", g.StartTurn+1)
	}
	if _, err := g.TurnAt(0).Explore(); err == nil {
		t.Fatal("un turno con fichas en la bolsa no se analiza como final")
	}
}

func TestEndgameAnalysisSortsLinesBySpread(t *testing.T) {
	e := analyzed(t, playedEndgame(t))
	for i := 1; i < e.LineCount(); i++ {
		if e.LineAt(i).Spread > e.LineAt(i-1).Spread {
			t.Fatalf("líneas desordenadas en %d", i)
		}
	}
}

func TestEndgameAnalysisIncludesThePlayedMove(t *testing.T) {
	g := playedEndgame(t)
	turn := g.TurnAt(g.StartTurn)
	e := analyzed(t, g)
	for i := 0; i < e.LineCount(); i++ {
		if e.LineAt(i).Description == turn.MarkAt(0).Description {
			return
		}
	}
	t.Fatalf("falta la jugada %s", turn.MarkAt(0).Description)
}

func analyzed(t *testing.T, g *Game) *Endgame {
	e := run(t, analysis(t, explorer(t, g)), &counter{})
	if e.LineCount() == 0 || e.Depth < 1 {
		t.Fatalf("análisis vacío: %+v", e)
	}
	return e
}

func playedEndgame(t *testing.T) *Game {
	testenv.Init(t)
	c, err := classic.NewEndgameSearch(0, 0, -classic.LeadLimit, classic.LeadLimit, classic.QAnywhere).Find()
	if err != nil {
		t.Fatal(err)
	}
	for !c.Status().Over {
		playClassicTurn(t, c)
	}
	text, err := c.Log()
	if err != nil {
		t.Fatal(err)
	}
	return mustParse(t, text)
}

func TestEndgameLinesCarryTheFinalDiscount(t *testing.T) {
	e := analyzed(t, playedEndgame(t))
	for i := 0; i < e.LineCount(); i++ {
		l := e.LineAt(i)
		closed := l.MoverLeft == "" || l.OtherLeft == ""
		if closed && l.MoverDelta != -l.OtherDelta || !closed && (l.MoverDelta >= 0 || l.OtherDelta >= 0) {
			t.Fatalf("%s: descuento imposible %+v", l.Description, *l)
		}
	}
}
