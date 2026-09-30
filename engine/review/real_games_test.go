package review

import (
	"strings"
	"testing"

	"lexico/engine/internal/testenv"
	"lexico/engine/modes/classic"
	"lexico/engine/modes/duplicate"
)

func TestRealClassicGame(t *testing.T) {
	testenv.Init(t)
	g, err := classic.Start("HastyBot")
	if err != nil {
		t.Fatal(err)
	}
	for !g.Status().Over {
		playClassicTurn(t, g)
	}
	checkLog(t, g.Log)
}

func TestRealDuplicateGame(t *testing.T) {
	testenv.Init(t)
	d, err := duplicate.Start()
	if err != nil {
		t.Fatal(err)
	}
	for i := 0; i < 60 && !d.Status().Over; i++ {
		if draw, err := d.DrawRack(); err != nil || draw.GameOver {
			break
		}
		d.TimeOut()
	}
	checkLog(t, d.Log)
}

func playClassicTurn(t *testing.T, g *classic.Game) {
	var err error
	if g.Status().HumanToMove {
		_, err = g.Play("pasar")
	} else {
		_, err = g.PlayBot()
	}
	if err != nil {
		t.Fatal(err)
	}
}

func checkLog(t *testing.T, log func() (string, error)) {
	t.Helper()
	text, err := log()
	if err != nil {
		t.Fatal(err)
	}
	g := mustParse(t, text)
	for i := 0; i < g.TurnCount(); i++ {
		if n := len(strings.Fields(g.TurnAt(i).Board)); n != size*size {
			t.Fatalf("turno %d: %d casillas", i+1, n)
		}
	}
	t.Logf("%s: %d turnos, %d-%d %s", g.Mode, g.TurnCount(), g.MyScore, g.OpponentScore, g.Outcome)
}
