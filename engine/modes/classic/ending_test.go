package classic

import (
	"strings"
	"testing"

	"github.com/domino14/macondo/turnplayer"

	"lexico/engine/internal/core"
)

func TestGoingOutMovesRackPoints(t *testing.T) {
	c := newTestGame(t)
	playTurns(t, c, 200)
	r, last := c.Result(), c.MoveAt(c.MoveCount()-1)
	if r.EndReason != EndOut || r.HumanDelta+r.BotDelta != 0 {
		t.Fatalf("%+v", r)
	}
	checkFinalScores(t, c, last)
	checkFinalScores(t, mustReload(t, c), last)
}

func TestFourPassesEndTheGame(t *testing.T) {
	c := newTestGame(t)
	human, bot := core.RackValue(c.g, c.humanIdx), core.RackValue(c.g, c.botIdx())
	for i := 0; i < passesToEnd; i++ {
		mustPass(t, c)
	}
	r := c.Result()
	if r == nil || r.EndReason != EndPasses || r.HumanDelta != -human || r.BotDelta != -bot {
		t.Fatalf("%+v", r)
	}
	checkFinalScores(t, c, c.MoveAt(c.MoveCount()-1))
	checkFinalScores(t, mustReload(t, c), c.MoveAt(c.MoveCount()-1))
}

func TestTwelveNeutralTurnsEndTheGame(t *testing.T) {
	c := newTestGame(t)
	for i := 0; i < neutralToEnd; i++ {
		if c.Status().Over {
			t.Fatalf("terminó en el turno %d: %+v", i, c.Result())
		}
		mustNeutral(t, c)
	}
	if r := c.Result(); r == nil || r.EndReason != EndNeutral {
		t.Fatalf("%+v", r)
	}
}

func checkFinalScores(t *testing.T, c *Game, last *Move) {
	t.Helper()
	r := c.Result()
	if r.HumanScore != last.HumanTotal+r.HumanDelta || r.BotScore != last.BotTotal+r.BotDelta {
		t.Fatalf("%+v tras %+v", r, last)
	}
}

func mustNeutral(t *testing.T, c *Game) {
	t.Helper()
	if !c.Status().HumanToMove {
		mustPass(t, c)
		return
	}
	if _, err := c.Play("cambiar " + strings.Fields(c.Rack())[0]); err != nil {
		t.Fatal(err)
	}
}

func mustPass(t *testing.T, c *Game) {
	t.Helper()
	tp := &turnplayer.BaseTurnPlayer{Game: c.g}
	m, err := tp.NewPassMove(c.g.PlayerOnTurn())
	if err == nil {
		_, err = c.apply(m, core.PlayOf(m), nil)
	}
	if err != nil {
		t.Fatal(err)
	}
}
