package review

import (
	"testing"

	"lexico/engine/internal/testenv"
	"lexico/engine/modes/duplicate"
)

func TestExchangesMatchWhateverTheOrder(t *testing.T) {
	if !sameMove("(Cambiar IQXC)", "(Cambiar QXIC)") || !sameMove("(Cambiar [CH]A?)", "(Cambiar ?A[CH])") {
		t.Fatal("mismo cambio en otro orden")
	}
	if sameMove("(Cambiar IQX)", "(Cambiar IQXC)") || sameMove("H8 CASA (12 pts)", "8H CASA (12 pts)") {
		t.Fatal("jugadas distintas")
	}
}

func TestExchangedRackIsRankedInAnyOrder(t *testing.T) {
	turn := mustParse(t, classicLog).TurnAt(0)
	turn.candidates = []*Move{{Description: "(Cambiar CIQX)"}}
	if turn.mark("Tú", &Move{Description: "(Cambiar IQXC)"}).Rank != 0 {
		t.Fatal("el cambio quedó sin puesto")
	}
}

func TestClassicRankingListsEveryMove(t *testing.T) {
	turn := preEndgameGame(t).TurnAt(0)
	ranked := rank(t, turn)
	if ranked.CandidateCount() <= turn.CandidateCount() || ranked.MarkAt(0).Rank < 0 || !ranked.CandidateAt(0).HasEquity {
		t.Fatalf("%d jugadas, puesto %d", ranked.CandidateCount(), ranked.MarkAt(0).Rank)
	}
	for i := 1; i < ranked.CandidateCount(); i++ {
		if ranked.CandidateAt(i).Equity > ranked.CandidateAt(i-1).Equity {
			t.Fatalf("desordenadas en %d", i)
		}
	}
}

func TestDuplicateRankingKeepsTheMasterFirst(t *testing.T) {
	testenv.Init(t)
	d, err := duplicate.Start()
	if err != nil {
		t.Fatal(err)
	}
	if draw, err := d.DrawRack(); err != nil || draw.GameOver {
		t.Fatal("sin atril", err)
	}
	d.TimeOut()
	text, err := d.Log()
	if err != nil {
		t.Fatal(err)
	}
	turn := mustParse(t, text).TurnAt(0)
	ranked := rank(t, turn)
	if ranked.MarkAt(0).Rank != 0 || ranked.CandidateAt(0).Description != turn.CandidateAt(0).Description {
		t.Fatalf("máster en el puesto %d: %s", ranked.MarkAt(0).Rank, ranked.CandidateAt(0).Description)
	}
}

func rank(t *testing.T, turn *Turn) *Turn {
	t.Helper()
	ranked, err := turn.Rank()
	if err != nil {
		t.Fatal(err)
	}
	return ranked
}
