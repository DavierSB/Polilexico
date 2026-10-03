package duplicate

import (
	"strings"
	"testing"
	"time"

	"github.com/domino14/macondo/move"
	"github.com/domino14/word-golib/tilemapping"
)

func TestRackRulesUpToRound15(t *testing.T) {
	d := newTestGame(t)
	for rack, want := range map[string]bool{
		"AEIOUBC": true, "AEIOUAB": false, "BCDFGHA": false, "BCDFGAE": true,
		"AEIOU??": true, "BCDFG??": true, "ABCD": true, "BCDFG": true,
	} {
		setRack(t, d, rack)
		if got := d.rackIsValid(false); got != want {
			t.Errorf("%s: válido %v, se esperaba %v", rack, got, want)
		}
	}
}

func TestRackRulesFromRound16IgnoreBlanks(t *testing.T) {
	d := newTestGame(t)
	for rack, want := range map[string]bool{
		"?BCDFGH": false, "?AEIOUA": false, "ABCDFGH": true, "EAIOUA?": false, "??AB": true,
	} {
		setRack(t, d, rack)
		if got := d.rackIsValid(true); got != want {
			t.Errorf("%s: válido %v, se esperaba %v", rack, got, want)
		}
	}
}

func TestReducedMinimumFromRound16(t *testing.T) {
	d := newTestGame(t)
	d.turn = reducedFromTurn - 1
	if d.reducedMinimum() {
		t.Fatal("mínimo reducido antes de la ronda 16")
	}
	d.turn = reducedFromTurn
	if !d.reducedMinimum() {
		t.Fatal("sin mínimo reducido en la ronda 16")
	}
}

func TestReducedMinimumWhenOneVowelIsLeft(t *testing.T) {
	d := newTestGame(t)
	keepVowels(t, d, 1)
	if !d.reducedMinimum() || !d.poolIsValid() {
		t.Fatalf("reducido %v, bolsa válida %v", d.reducedMinimum(), d.poolIsValid())
	}
	draw := mustDraw(t, d)
	if draw.GameOver {
		t.Fatalf("terminó con una vocal en la bolsa: %+v", *draw)
	}
	if vowels, _ := d.countPure(d.g.RackFor(masterIdx).TilesOn()); vowels != 1 {
		t.Fatalf("atril %s sin la última vocal", draw.Rack)
	}
}

func TestGameEndsWithoutPureVowels(t *testing.T) {
	d := newTestGame(t)
	keepVowels(t, d, 0)
	if draw := mustDraw(t, d); !draw.GameOver || draw.EndReason != EndNoValidRack {
		t.Fatalf("%+v", *draw)
	}
}

func TestMasterTiebreaksFollowFILE(t *testing.T) {
	d := newTestGame(t)
	alph := d.g.Alphabet()
	cases := []struct{ name, better, worse string }{
		{"sin comodín", "8D CASA", "8D CAsA"},
		{"menos fichas", "8D CASA", "8D CASAS"},
		{"orden alfabético", "8D ACASO", "8D CASAS"},
		{"CH tras C", "8D CZAR", "8D [CH]ALE"},
		{"horizontal antes que vertical", "8H CASA", "H8 CASA"},
		{"horizontal: columna más cercana a la 1", "9A CASA", "8B CASA"},
		{"vertical: fila más cercana a la A", "B8 CASA", "A9 CASA"},
	}
	for _, c := range cases {
		better, worse := scoredMove(c.better, alph), scoredMove(c.worse, alph)
		if d.compareMasterPlays(better, worse) >= 0 || d.compareMasterPlays(worse, better) <= 0 {
			t.Errorf("%s: %s debería ir antes que %s", c.name, c.better, c.worse)
		}
	}
}

func TestMasterTiebreakEquityBeforeTiles(t *testing.T) {
	d := newTestGame(t)
	alph := d.g.Alphabet()
	short, long := scoredMove("8D CASA", alph), scoredMove("8D CASAS", alph)
	long.SetEquity(short.Equity() + 1)
	if d.compareMasterPlays(long, short) >= 0 {
		t.Fatal("la equity no decidió antes que el número de fichas")
	}
}

func TestMatchShowsEachInvalidRack(t *testing.T) {
	m, clock := newTestMatch(t)
	enterTestDraw(t, m, &Draw{Rack: m.Rack(), Redrawn: true, InvalidCount: 2, invalidRacks: []string{"AEIOUAE", "BCDFGHJ"}})
	for _, rack := range []string{"AEIOUAE", "BCDFGHJ"} {
		if m.Phase() != PhaseInvalidRack || m.InvalidRack() != rack {
			t.Fatalf("fase %s, mano %q, se esperaba %q", m.Phase(), m.InvalidRack(), rack)
		}
		clock.Advance(InvalidRackSeconds * time.Second)
	}
	if m.Phase() != PhasePlaying {
		t.Fatalf("fase %s", m.Phase())
	}
}

func TestMatchSummarizesManyInvalidRacks(t *testing.T) {
	m, clock := newTestMatch(t)
	enterTestDraw(t, m, &Draw{Rack: "ABCDEFG", Redrawn: true, InvalidCount: maxShownInvalidRacks + 1})
	if m.Phase() != PhaseManyInvalid || m.Rack() != "ABCDEFG" {
		t.Fatalf("fase %s, atril %q", m.Phase(), m.Rack())
	}
	clock.Advance(InvalidRackSeconds * time.Second)
	if m.Phase() != PhasePlaying {
		t.Fatalf("fase %s", m.Phase())
	}
}

func setRack(t *testing.T, d *Game, rack string) {
	t.Helper()
	if err := d.g.SetRackFor(masterIdx, tilemapping.RackFromString(rack, d.g.Alphabet())); err != nil {
		t.Fatal(err)
	}
}

func keepVowels(t *testing.T, d *Game, keep int) {
	t.Helper()
	d.g.ThrowRacksIn()
	ld := d.g.Bag().LetterDistribution()
	var vowels []tilemapping.MachineLetter
	for _, ml := range d.g.Bag().Peek() {
		if ml != 0 && ml.IsVowel(ld) {
			vowels = append(vowels, ml)
		}
	}
	if err := d.g.Bag().RemoveTiles(vowels[keep:]); err != nil {
		t.Fatal(err)
	}
	if _, err := d.g.SetRandomRack(masterIdx, nil); err != nil {
		t.Fatal(err)
	}
}

func scoredMove(text string, alph *tilemapping.TileMapping) *move.Move {
	coords, word, _ := strings.Cut(text, " ")
	return move.NewScoringMoveSimple(20, coords, word, "", alph)
}

func enterTestDraw(t *testing.T, m *Match, draw *Draw) {
	t.Helper()
	m.mu.Lock()
	defer m.mu.Unlock()
	m.enterDraw(draw)
	m.afterChange()
}

func TestGameEndsAfterTheAgreedRounds(t *testing.T) {
	d := newTestGame(t)
	d.SetMaxRounds(3)
	playGame(t, d)
	if r := d.Result(); r == nil || r.Turns != 3 {
		t.Fatalf("%+v", r)
	}
	if _, err := d.DrawRack(); err != errGameOver {
		t.Fatalf("siguió tras la última ronda: %v", err)
	}
}

func TestRoundLimitSurvivesReload(t *testing.T) {
	d := newTestGame(t)
	d.SetMaxRounds(4)
	playTurn(t, d, false)
	if loaded := mustReload(t, d); loaded.MaxRounds() != 4 {
		t.Fatalf("rondas %d", loaded.MaxRounds())
	}
}

func TestProgressOfASavedMatch(t *testing.T) {
	m, clock := newTestMatch(t)
	for i := 0; i < 2; i++ {
		mustShowRack(t, m, clock)
		mustPropose(t, m, masterInput(m.game))
		clock.Advance(CancelSeconds * time.Second)
	}
	text, err := m.Save()
	if err != nil {
		t.Fatal(err)
	}
	p, err := ReadProgress(text)
	status := m.game.Status()
	if err != nil || p.Round != 3 || p.HumanTotal != status.HumanTotal || p.MasterTotal != status.MasterTotal {
		t.Fatalf("%+v %v", p, err)
	}
}
