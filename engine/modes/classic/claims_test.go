package classic

import (
	"testing"

	"github.com/domino14/word-golib/tilemapping"

	"lexico/engine/internal/core"
	"lexico/engine/internal/testenv"
	"lexico/engine/internal/timing"
)

func TestPhonyStandsUntilClaimed(t *testing.T) {
	c := phonyPlayed(t)
	if c.MoveAt(0).Kind != core.KindPlay || c.MoveAt(0).Score == 0 || !c.Claimable() {
		t.Fatalf("%+v %v", c.MoveAt(0), c.Claimable())
	}
}

func TestUpheldClaimWithdrawsThePlay(t *testing.T) {
	c := phonyPlayed(t)
	if _, err := c.Play("reclamar"); err != nil {
		t.Fatal(err)
	}
	m, claim := c.MoveAt(0), c.LastClaim()
	if m.Kind != core.KindInvalid || m.HumanTotal != 0 || !claim.Upheld || claim.ByHuman || claim.Invalid != "TPN" {
		t.Fatalf("%+v %+v", m, claim)
	}
	if c.Status().HumanToMove || c.Claimable() || core.RackText(c.g, 0) != core.RackText(rackGame(t).g, 0) {
		t.Fatalf("turno o atril: %s", core.RackText(c.g, 0))
	}
}

func TestFailedClaimCostsNothing(t *testing.T) {
	c := rackGame(t)
	if _, err := c.Play("h8 PATIO"); err != nil {
		t.Fatal(err)
	}
	score := c.MoveAt(0).Score
	if _, err := c.Play("reclamar"); err != nil {
		t.Fatal(err)
	}
	if c.LastClaim().Upheld || c.MoveAt(0).Kind != core.KindPlay || c.Status().HumanScore != score || c.Status().HumanToMove || c.Claimable() {
		t.Fatalf("%+v %+v", c.LastClaim(), c.Status())
	}
}

func TestNothingToClaimAtStart(t *testing.T) {
	c := newClaimGame(t)
	if _, err := c.Play("reclamar"); err == nil {
		t.Fatal("reclamó sin jugada")
	}
}

func TestClaimSurvivesSave(t *testing.T) {
	c := mustReload(t, phonyPlayed(t))
	if !c.Claimable() {
		t.Fatal("se perdió la reclamación pendiente")
	}
	if _, err := c.Play("reclamar"); err != nil || !c.LastClaim().Upheld {
		t.Fatal(err)
	}
	if c = mustReload(t, c); c.MoveAt(0).Kind != core.KindInvalid || c.Status().HumanScore != 0 {
		t.Fatalf("%+v", c.MoveAt(0))
	}
}

func TestDisablingClaimsWithdrawsAPendingPhony(t *testing.T) {
	c := phonyPlayed(t)
	c.setClaims(false)
	if c.Claims() || c.MoveAt(0).Kind != core.KindInvalid || c.LastClaim() == nil {
		t.Fatalf("%+v", c.MoveAt(0))
	}
	giveRack(t, c, 1)
	if _, err := c.Play("h8 TPN"); err != nil || c.MoveAt(1).Kind != core.KindInvalid {
		t.Fatalf("sin reclamación vuelve la comprobación automática: %v", err)
	}
}

func TestClaimGamesReachTheEnd(t *testing.T) {
	for _, claims := range []bool{true, false} {
		c := newClaimGame(t)
		c.setClaims(claims)
		playOut(t, c)
		if r := c.Result(); r == nil || r.EndReason == "" {
			t.Fatalf("claims=%v: %+v", claims, r)
		}
	}
}

func playOut(t *testing.T, c *Game) {
	t.Helper()
	for i := 0; i < 200 && !c.Status().Over; i++ {
		if _, err := c.Play(nextInput(c)); err != nil {
			t.Fatal(err)
		}
	}
}

func nextInput(c *Game) string {
	if c.FinalPass() {
		return "pasar"
	}
	return testenv.Input(c.bot.GenerateMoves(1)[0])
}

func phonyPlayed(t *testing.T) *Game {
	t.Helper()
	c := rackGame(t)
	if _, err := c.Play("h8 TPN"); err != nil {
		t.Fatal(err)
	}
	return c
}

func rackGame(t *testing.T) *Game {
	t.Helper()
	c := newClaimGame(t)
	giveRack(t, c, 0)
	return c
}

func giveRack(t *testing.T, c *Game, player int) {
	t.Helper()
	if err := c.g.SetRackFor(player, tilemapping.RackFromString("TPNAEIO", c.g.Alphabet())); err != nil {
		t.Fatal(err)
	}
}

func newClaimGame(t *testing.T) *Game {
	t.Helper()
	c := newLocalGame(t, StarterHuman)
	c.invalidLosesTurn = true
	c.setClaims(true)
	return c
}

func TestMirrorsReadAClaim(t *testing.T) {
	c := phonyPlayed(t)
	if _, err := c.Play("reclamar"); err != nil {
		t.Fatal(err)
	}
	m := startMatch(c, nil, timing.NewFake(), newTestListener(), false)
	t.Cleanup(m.Close)
	for _, first := range []bool{true, false} {
		mirror := mirrorOf(t, m, first)
		if g := mirror.Game(); g.MoveAt(0).Kind != core.KindInvalid || !g.LastClaim().Upheld || !g.Claims() || g.Status().HumanToMove {
			t.Fatalf("first=%v: %+v", first, g.MoveAt(0))
		}
	}
}

func TestMirrorsSeeAPendingClaim(t *testing.T) {
	m := startMatch(phonyPlayed(t), nil, timing.NewFake(), newTestListener(), false)
	t.Cleanup(m.Close)
	if g := mirrorOf(t, m, false).Game(); !g.Claimable() {
		t.Fatal("el rival no ve la jugada pendiente")
	}
}
