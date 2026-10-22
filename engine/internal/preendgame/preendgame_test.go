package preendgame

import (
	"testing"
	"time"

	"github.com/domino14/macondo/move"
	"github.com/domino14/word-golib/tilemapping"

	"lexico/engine/internal/core"
	"lexico/engine/internal/testenv"
)

func TestPartners(t *testing.T) {
	ls := mustLetters(t)
	cases := map[string]bool{
		"UE": true, "UI": true, "UAD": true, "UA": false, "UD": false, "U": false,
		"?E": true, "?A": false, "??": true, "?AD": true, "U?": true, "UA?": true,
		"EAD": false, "": false,
	}
	for tiles, want := range cases {
		if got := ls.partnered(rackOf(t, tiles)); got != want {
			t.Errorf("%q: compañeras %v, esperado %v", tiles, got, want)
		}
	}
}

func TestFootprintsTouchingSideBySideAreNotApart(t *testing.T) {
	a := footprint{}
	a.cells.set(7, 7, 15)
	a.halo.surround(7, 7, 15)
	side, diagonal := footprint{}, footprint{}
	side.cells.set(7, 8, 15)
	diagonal.cells.set(8, 8, 15)
	if a.apart(side) || !a.apart(diagonal) {
		t.Fatal("los lados se tocan; en diagonal no")
	}
}

func TestDrawOddsCapAtOne(t *testing.T) {
	exch := move.NewExchangeMove(wordOf(t, "QU"), nil, nil)
	cases := []struct {
		m    *move.Move
		base int
		want float64
	}{{exch, 4, 0.5}, {exch, 1, 1}, {move.NewPassMove(nil, nil), 3, 0}}
	for _, c := range cases {
		if got := drawOdds(c.m, c.base); got != c.want {
			t.Errorf("probabilidad %v, esperado %v", got, c.want)
		}
	}
}

func TestPreEndgameMovesAreLegalAndTimely(t *testing.T) {
	c := quick()
	for game := range 6 {
		sp := preEndgame(t)
		for turn := Prepare(sp.Game); turn != nil && !sp.Over(); turn = Prepare(sp.Game) {
			start := time.Now()
			m, err := turn.Move(c)
			if err != nil {
				t.Fatal(err)
			}
			if spent := time.Since(start); spent > c.ScanBudget+3*c.Endgame.Budget {
				t.Errorf("partida %d: tardó %v", game, spent)
			}
			if err := sp.Play(m); err != nil {
				t.Fatalf("partida %d: jugada ilegal %v: %v", game, m.ShortDescription(), err)
			}
		}
	}
}

func quick() Config {
	c := Default
	c.Endgame.Budget, c.ScanBudget = 300*time.Millisecond, 300*time.Millisecond
	return c
}

func preEndgame(t *testing.T) *core.SelfPlay {
	testenv.Init(t)
	sp, err := core.NewSelfPlay()
	if err != nil {
		t.Fatal(err)
	}
	for !sp.Over() && sp.Game.Bag().TilesRemaining() > reach {
		if err := sp.Play(core.CopyMove(sp.Best())); err != nil {
			t.Fatal(err)
		}
	}
	return sp
}

func mustLetters(t *testing.T) letters {
	testenv.Init(t)
	ls, err := newLetters()
	if err != nil {
		t.Fatal(err)
	}
	return ls
}

func rackOf(t *testing.T, tiles string) *tilemapping.Rack {
	ld, err := core.Distribution()
	if err != nil {
		t.Fatal(err)
	}
	r := tilemapping.NewRack(ld.TileMapping())
	r.Set(wordOf(t, tiles))
	return r
}

func wordOf(t *testing.T, tiles string) tilemapping.MachineWord {
	testenv.Init(t)
	ld, err := core.Distribution()
	if err != nil {
		t.Fatal(err)
	}
	var w tilemapping.MachineWord
	for _, ch := range tiles {
		ml, err := ld.TileMapping().Val(string(ch))
		if err != nil {
			t.Fatal(err)
		}
		w = append(w, ml)
	}
	return w
}

func TestDealsGiveMeTheQAndTheRivalSeven(t *testing.T) {
	b := &preBot{letters: mustLetters(t)}
	unseenTiles := rackOf(t, "QUEAADRSOI")
	deals, err := b.deals(unseenTiles, 3)
	if err != nil || len(deals) == 0 {
		t.Fatalf("sin repartos: %v", err)
	}
	seen := map[string]bool{}
	for _, d := range deals {
		if d.mine.NumTiles() != 3 || d.mine.CountOf(b.letters.q) != 1 || d.theirs.NumTiles() != 7 {
			t.Fatalf("reparto mal hecho: %s / %s", d.mine.String(), d.theirs.String())
		}
		if seen[d.mine.String()] {
			t.Fatalf("reparto repetido: %s", d.mine.String())
		}
		seen[d.mine.String()] = true
	}
}

func TestDealsAreCapped(t *testing.T) {
	b := &preBot{letters: mustLetters(t)}
	deals, err := b.deals(rackOf(t, "QABCDEFGHIJLMN"), 7)
	if err != nil || len(deals) != maxDeals {
		t.Fatalf("%d repartos, esperado %d (%v)", len(deals), maxDeals, err)
	}
}
