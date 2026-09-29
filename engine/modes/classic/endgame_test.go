package classic

import (
	"errors"
	"strings"
	"testing"
	"time"

	"lexico/engine/internal/testenv"
	"lexico/engine/review"
)

func TestEndgameStartsOnYourTurnWithTheLead(t *testing.T) {
	c := findEndgame(t, EndgameMaxBag, EndgameMinLead, EndgameMaxLead)
	s := c.Status()
	lead := s.HumanScore - s.BotScore
	if !s.HumanToMove || s.BagCount > EndgameMaxBag || lead < EndgameMinLead || lead > EndgameMaxLead {
		t.Fatalf("%+v (ventaja %d)", *s, lead)
	}
	if c.log.StartTurn != c.MoveCount() {
		t.Fatalf("la revisión empieza en el turno %d, no en el tuyo (%d)", c.log.StartTurn, c.MoveCount())
	}
	last := c.MoveAt(c.MoveCount() - 1)
	if last.ByHuman || last.HumanTotal != s.HumanScore || last.BotTotal != s.BotScore {
		t.Fatalf("la última jugada debería ser del rival, con el marcador actual: %+v", *last)
	}
}

func TestEndgameIsAClassicGameToTheEnd(t *testing.T) {
	c := findEndgame(t, EndgameMaxBag, -LeadLimit, LeadLimit)
	loaded := mustReload(t, c)
	playTurns(t, loaded, 200)
	r := loaded.Result()
	if r == nil || !strings.Contains(r.RecordPath, ModeEndgame+"-") {
		t.Fatalf("%+v", r)
	}
	g, err := review.Open(r.RecordPath)
	if err != nil || g.Mode != review.ModeEndgame || g.TurnCount() != loaded.MoveCount() || g.StartTurn != c.log.StartTurn {
		t.Fatalf("registro: %+v %v", g, err)
	}
}

func TestEndgameTurnsNameYouAsThePlayerOnTurn(t *testing.T) {
	c := findEndgame(t, EndgameMaxBag, -LeadLimit, LeadLimit)
	for i, turn := range c.log.Turns {
		if (turn.PlayerName == humanName) != c.MoveAt(i).ByHuman {
			t.Fatalf("turno %d de %s, jugada %+v", i+1, turn.PlayerName, *c.MoveAt(i))
		}
	}
}

func TestEndgameRejectsAnEmptyLeadRange(t *testing.T) {
	testenv.Init(t)
	if _, err := NewEndgameSearch(EndgameMaxBag, 10, -10).Find(); !errors.Is(err, errLeadRange) {
		t.Fatal(err)
	}
}

func TestEndgameSearchStops(t *testing.T) {
	testenv.Init(t)
	s := NewEndgameSearch(0, LeadLimit, LeadLimit)
	time.AfterFunc(300*time.Millisecond, s.Stop)
	if _, err := s.Find(); !errors.Is(err, errSearchStopped) {
		t.Fatal(err)
	}
}

func findEndgame(t *testing.T, maxBag, minLead, maxLead int) *Game {
	t.Helper()
	testenv.Init(t)
	c, err := NewEndgameSearch(maxBag, minLead, maxLead).Find()
	if err != nil {
		t.Fatal(err)
	}
	return c
}
