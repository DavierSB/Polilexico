package duplicate

import (
	"testing"

	"github.com/domino14/word-golib/tilemapping"

	"lexico/engine/internal/core"
	"lexico/engine/internal/testenv"
)

func TestFullGame(t *testing.T) {
	d := newTestGame(t)
	playGame(t, d)
	r := d.Result()
	if r == nil || r.Turns == 0 {
		t.Fatal("no terminó")
	}
	if r.HumanTotal >= r.MasterTotal && r.Hits < r.Turns {
		t.Fatalf("marcador imposible: %+v", *r)
	}
	t.Logf("%+v", *r)
}

func TestValidateDoesNotPlay(t *testing.T) {
	d := newTestGame(t)
	mustDraw(t, d)
	attempt, err := d.Validate(masterInput(d))
	if err != nil || attempt.Kind != core.KindPlay || attempt.Immediate {
		t.Fatalf("%+v %v", attempt, err)
	}
	if !d.Status().InTurn || d.TurnCount() != 0 {
		t.Fatal("Validate cerró el turno")
	}
}

func TestHitWhenPlayingTheMasterMove(t *testing.T) {
	d := newTestGame(t)
	mustDraw(t, d)
	turn, err := d.Confirm(masterInput(d))
	if err != nil || !turn.Hit || turn.HumanScore != turn.MasterScore || turn.Hits != 1 {
		t.Fatalf("%+v %v", turn, err)
	}
}

func TestNoExchange(t *testing.T) {
	d := newTestGame(t)
	mustDraw(t, d)
	if _, err := d.Validate("cambiar ABC"); err != errNoExchange {
		t.Fatalf("%v", err)
	}
}

func TestInvalidPlayLosesTurn(t *testing.T) {
	d := gameWithRack(t, "TPNAEIO")
	if _, err := d.Validate("h8 TPN"); err == nil {
		t.Fatal("sin SetInvalidPlayLosesTurn aceptó una palabra inválida")
	}
	d.SetInvalidPlayLosesTurn(true)
	attempt, err := d.Validate("h8 TPN")
	if err != nil || attempt.Kind != core.KindInvalid || !attempt.Immediate {
		t.Fatalf("%+v %v", attempt, err)
	}
	turn, err := d.Confirm("h8 TPN")
	if err != nil || turn.HumanKind != core.KindInvalid || turn.HumanTotal != 0 {
		t.Fatalf("%+v %v", turn, err)
	}
}

func TestSaveMidTurnAndLoad(t *testing.T) {
	d := newTestGame(t)
	for i := 0; i < 5; i++ {
		playTurn(t, d, false)
	}
	draw := mustDraw(t, d)
	loaded := mustReload(t, d)
	if again := mustDraw(t, loaded); again.Rack != draw.Rack || *loaded.Status() != *d.Status() {
		t.Fatalf("no coincide: %q/%q\n%+v\n%+v", draw.Rack, again.Rack, *d.Status(), *loaded.Status())
	}
	playGame(t, loaded)
	if !loaded.Status().Over {
		t.Fatal("la partida recargada no llegó al final")
	}
}

func newTestGame(t *testing.T) *Game {
	t.Helper()
	testenv.Init(t)
	d, err := Start()
	if err != nil {
		t.Fatal(err)
	}
	return d
}

func gameWithRack(t *testing.T, rack string) *Game {
	t.Helper()
	d := newTestGame(t)
	if err := d.g.SetRackFor(masterIdx, tilemapping.RackFromString(rack, d.g.Alphabet())); err != nil {
		t.Fatal(err)
	}
	d.g.ThrowRacksInFor(1 - masterIdx)
	mustDraw(t, d)
	return d
}

func playGame(t *testing.T, d *Game) {
	t.Helper()
	for i := 0; i < 60 && playTurn(t, d, i%5 == 4); i++ {
	}
}

func playTurn(t *testing.T, d *Game, timeOut bool) bool {
	t.Helper()
	if d.Status().Over || mustDraw(t, d).GameOver {
		return false
	}
	if _, err := closeTurn(d, timeOut); err != nil {
		t.Fatal(err)
	}
	return true
}

func closeTurn(d *Game, timeOut bool) (*Turn, error) {
	if timeOut {
		return d.TimeOut()
	}
	return d.Confirm(masterInput(d))
}

func mustDraw(t *testing.T, d *Game) *Draw {
	t.Helper()
	draw, err := d.DrawRack()
	if err != nil {
		t.Fatal(err)
	}
	if draw.Redrawn {
		t.Logf("manos inválidas: %v (%d) -> %s", draw.invalidRacks, draw.InvalidCount, draw.Rack)
	}
	return draw
}

func masterInput(d *Game) string {
	return testenv.Input(d.plays[0])
}

func mustReload(t *testing.T, d *Game) *Game {
	t.Helper()
	saved, err := d.Save()
	if err != nil {
		t.Fatal(err)
	}
	loaded, err := Load(saved)
	if err != nil {
		t.Fatal(err)
	}
	return loaded
}
