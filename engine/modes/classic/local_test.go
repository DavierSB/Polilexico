package classic

import (
	"testing"
	"time"

	"lexico/engine/internal/core"
	"lexico/engine/internal/testenv"
	"lexico/engine/internal/timing"
)

func TestLocalBothPlayersPlay(t *testing.T) {
	c := newLocalGame(t, StarterHuman)
	for i := 0; i < 4; i++ {
		if _, err := c.Play(testenv.Input(c.bot.GenerateMoves(1)[0])); err != nil {
			t.Fatalf("jugada %d: %v", i, err)
		}
	}
	if c.MoveCount() != 4 || !c.MoveAt(0).ByHuman || c.MoveAt(1).ByHuman {
		t.Fatalf("jugadas: %d", c.MoveCount())
	}
}

func TestLocalShowsTheRivalRack(t *testing.T) {
	c := newLocalGame(t, StarterBot)
	if c.OpponentRack() == "" || c.Status().HumanToMove {
		t.Fatal("no se ve el atril de quien juega")
	}
}

func TestLocalMatchHasNoBot(t *testing.T) {
	m := startMatch(newLocalGame(t, StarterBot), nil, timing.NewFake(), newTestListener(), false)
	t.Cleanup(m.Close)
	time.Sleep(100 * time.Millisecond)
	if m.Game().MoveCount() != 0 {
		t.Fatal("jugó un bot")
	}
	if _, err := m.Play(testenv.Input(m.game.bot.GenerateMoves(1)[0])); err != nil {
		t.Fatal(err)
	}
}

func TestLocalRivalLosesOnTime(t *testing.T) {
	clock := timing.NewFake()
	m := startMatch(newLocalGame(t, StarterBot), newClockPair(clock, 60_000, 30_000, 0, 0), clock, newTestListener(), false)
	t.Cleanup(m.Close)
	clock.Advance(90 * time.Second)
	if r := m.Game().Result(); r == nil || !r.RivalLostOnTime || r.LostOnTime || r.Outcome != core.OutcomeWin {
		t.Fatalf("%+v", r)
	}
}

func TestLocalSurvivesSave(t *testing.T) {
	c := mustReload(t, newLocalGame(t, StarterHuman))
	if !c.Local() || c.Host() != "Ana" || c.Opponent() != "Beto" {
		t.Fatalf("%v %s %s", c.Local(), c.Host(), c.Opponent())
	}
}

func newLocalGame(t *testing.T, starter int) *Game {
	t.Helper()
	testenv.Init(t)
	c, err := StartLocal("Ana", "Beto", starter)
	if err != nil {
		t.Fatal(err)
	}
	return c
}

func TestMirrorRunsClocksButDecidesNothing(t *testing.T) {
	text := savedLocalMatch(t)
	m, err := LoadMirror(text, false, newTestListener())
	if err != nil {
		t.Fatal(err)
	}
	t.Cleanup(m.Close)
	time.Sleep(100 * time.Millisecond)
	if c := m.Clocks(); c == nil || c.Running != RunningBot || m.Game().Result() != nil {
		t.Fatalf("%+v %+v", c, m.Game().Result())
	}
}

func savedLocalMatch(t *testing.T) string {
	t.Helper()
	m := startMatch(newLocalGame(t, StarterBot), newClockPair(timing.NewFake(), 60_000, 0, 0, 120_000), timing.NewFake(), newTestListener(), true)
	t.Cleanup(m.Close)
	text, err := m.Save()
	if err != nil {
		t.Fatal(err)
	}
	return text
}

func TestMirrorOfAFinishedGameKeepsItsRecord(t *testing.T) {
	m := startMatch(newLocalGame(t, StarterBot), nil, timing.NewFake(), newTestListener(), true)
	t.Cleanup(m.Close)
	m.game.LoseOnTime()
	m.game.recordPath = ""
	text, err := m.Save()
	if err != nil {
		t.Fatal(err)
	}
	mirror, err := LoadMirror(text, true, newTestListener())
	if err != nil {
		t.Fatal(err)
	}
	t.Cleanup(mirror.Close)
	if r := mirror.Game().Result(); r == nil || r.RecordPath == "" {
		t.Fatalf("%+v", r)
	}
}

func TestLocalEndgameSeatsTheChosenStarter(t *testing.T) {
	for starter, humanToMove := range map[int]bool{StarterHuman: true, StarterBot: false} {
		testenv.Init(t)
		c, err := NewEndgameSearch(EndgameMinBag, EndgameMaxBag, -LeadLimit, LeadLimit, QAnywhere).FindLocal("Ana", "Luis", starter)
		if err != nil {
			t.Fatal(err)
		}
		if !c.Local() || c.Status().HumanToMove != humanToMove || c.Host() != "Ana" || c.Opponent() != "Luis" {
			t.Fatalf("salidor %d: %+v", starter, *c.Status())
		}
		for i, turn := range c.log.Turns {
			if (turn.PlayerName == "Ana") != c.MoveAt(i).ByHuman {
				t.Fatalf("turno %d de %s", i+1, turn.PlayerName)
			}
		}
	}
}

func TestLocalEndgameSurvivesSave(t *testing.T) {
	testenv.Init(t)
	c, err := NewEndgameSearch(EndgameMinBag, EndgameMaxBag, -LeadLimit, LeadLimit, QAnywhere).FindLocal("Ana", "Luis", StarterAny)
	if err != nil {
		t.Fatal(err)
	}
	loaded := mustReload(t, c)
	if !loaded.Local() || loaded.log.Mode != ModeEndgame || loaded.OpponentRack() == "" {
		t.Fatal("al recargar dejó de ser un final entre dos")
	}
}
