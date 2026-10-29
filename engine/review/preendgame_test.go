package review

import (
	"strings"
	"testing"

	"lexico/engine/internal/testenv"
)

const (
	preEndgameLog = "testdata/preendgame-log.json"
	swallowsQ     = 16
)

func TestPreEndgameTurnsAreTheOnesWithABag(t *testing.T) {
	g := preEndgameGame(t)
	for i := 0; i < g.TurnCount(); i++ {
		turn := g.TurnAt(i)
		if turn.PreEndgame && turn.Endgame {
			t.Fatalf("turno %d: pre-final y final a la vez", turn.Number)
		}
	}
	if g.TurnAt(0).PreEndgame || !g.TurnAt(swallowsQ).PreEndgame {
		t.Fatal("pre-final mal marcado")
	}
}

func TestPreEndgamePenalizesSwallowingTheQ(t *testing.T) {
	p := preEndgameOfTurn(t, swallowsQ)
	buen, single := findMove(p, "C1 BUEN"), findMove(p, "7I B...")
	if buen != nil && buen.Adjustment >= 0 {
		t.Fatalf("BUEN se traga la Q sin U y no se penaliza: %+v", *buen)
	}
	if single == nil || single.Adjustment != 0 {
		t.Fatalf("B... no debería penalizarse: %+v", single)
	}
	if p.Played == nil || p.PlayedRank < 0 {
		t.Fatalf("falta la jugada hecha: %+v", p)
	}
}

func TestPreEndgameMovesAreSortedByValue(t *testing.T) {
	p := preEndgameOfTurn(t, swallowsQ)
	for i := 1; i < p.MoveCount(); i++ {
		if p.Verdict == "" && p.MoveAt(i).Value > p.MoveAt(i-1).Value {
			t.Fatalf("jugadas desordenadas en %d", i)
		}
	}
}

func preEndgameOfTurn(t *testing.T, turn int) *PreEndgame {
	t.Helper()
	a, err := preEndgameGame(t).TurnAt(turn).AnalyzePreEndgame()
	if err != nil {
		t.Fatal(err)
	}
	c := &counter{}
	if err := a.Run(c); err != nil {
		t.Fatal(err)
	}
	if p := a.Latest(); p.Done && c.calls.Load() == 2 && p.MoveCount() > 0 {
		return p
	}
	t.Fatalf("análisis incompleto: %d avisos", c.calls.Load())
	return nil
}

func findMove(p *PreEndgame, prefix string) *PreEndgameMove {
	for i := 0; i < p.MoveCount(); i++ {
		if strings.HasPrefix(p.MoveAt(i).Description, prefix) {
			return p.MoveAt(i)
		}
	}
	if p.Played != nil && strings.HasPrefix(p.Played.Description, prefix) {
		return p.Played
	}
	return nil
}

func preEndgameGame(t *testing.T) *Game {
	testenv.Init(t)
	g, err := Open(preEndgameLog)
	if err != nil {
		t.Fatal(err)
	}
	return g
}
